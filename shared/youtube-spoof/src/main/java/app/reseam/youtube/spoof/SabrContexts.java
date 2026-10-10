// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.youtube.spoof;

import java.io.ByteArrayOutputStream;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Opaque web contexts for one native playback. Android owns PLAYBACK contexts; this store supplies
 * CONTENT_ADS contexts, which its parser ignores. Values and server sending policies are preserved.
 * Nothing is shared by video ID: simultaneous playback/prefetch instances have separate stores.
 */
final class SabrContexts {
    private static final int CONTENT_ADS = 4;
    private static final int KEEP_EXISTING = 2;
    private static final int MAX_CONTEXTS = 256;
    private static final int MAX_BYTES = 1024 * 1024;
    private final Map<Integer, Context> contexts = new LinkedHashMap<>();

    private record Context(byte[] value, boolean active) {}

    synchronized void update(byte[] message) {
        // SabrContextUpdate: type (1), scope (2), value (3), sendByDefault (4), writePolicy (5).
        int scope = (int) Proto.number(message, 2, 0);
        if (scope != CONTENT_ADS) return;
        int type = (int) Proto.number(message, 1, 0);
        byte[] value = Proto.bytes(message, 3);
        if (type <= 0 || value == null) throw new IllegalArgumentException("Invalid SABR context update");
        if (Proto.number(message, 5, 0) == KEEP_EXISTING && contexts.containsKey(type)) return;
        Context replacement = new Context(value, Proto.number(message, 4, 0) != 0);
        int bytes = contexts.entrySet().stream().filter(entry -> entry.getKey() != type)
                .mapToInt(entry -> entry.getValue().value.length).sum() + value.length;
        if (bytes > MAX_BYTES || !contexts.containsKey(type) && contexts.size() >= MAX_CONTEXTS) {
            throw new IllegalArgumentException("SABR context storage limit exceeded");
        }
        contexts.put(type, replacement);
    }

    synchronized void policy(byte[] message) {
        // SabrContextSendingPolicy: startPolicy (1), stopPolicy (2), discardPolicy (3).
        // Decode fully before applying a policy, so a malformed packed list cannot apply half an update.
        Map<Integer, Set<Integer>> operations = new LinkedHashMap<>();
        for (Proto.Field field : Proto.fields(message)) {
            if (field.number < 1 || field.number > 3) continue;
            Set<Integer> types = operations.computeIfAbsent(field.number, ignored -> new HashSet<>());
            if (field.wireType == Proto.WIRE_VARINT) types.add((int) field.varint);
            else if (field.wireType == Proto.WIRE_LENGTH) {
                int[] position = {0};
                while (position[0] < field.bytes.length) types.add((int) Proto.readVarint(field.bytes, position));
            }
        }
        for (int operation = 1; operation <= 3; operation++) {
            for (int type : operations.getOrDefault(operation, Set.of())) {
                Context context = contexts.get(type);
                if (context == null) continue;
                if (operation == 3) contexts.remove(type);
                else contexts.put(type, new Context(context.value, operation == 1));
            }
        }
    }

    synchronized byte[] append(byte[] streamerContext) {
        // StreamerContext: sabrContexts (5), unsentSabrContexts (6). Each context has type (1), value (2).
        Set<Integer> nativeTypes = new HashSet<>();
        for (Proto.Field field : Proto.fields(streamerContext)) {
            if (field.number == 5 && field.wireType == Proto.WIRE_LENGTH) {
                nativeTypes.add((int) Proto.number(field.bytes, 1, 0));
            } else if (field.number == 6 && field.wireType == Proto.WIRE_VARINT) {
                nativeTypes.add((int) field.varint);
            } else if (field.number == 6 && field.wireType == Proto.WIRE_LENGTH) {
                int[] position = {0};
                while (position[0] < field.bytes.length) nativeTypes.add((int) Proto.readVarint(field.bytes, position));
            }
        }
        ByteArrayOutputStream output = new ByteArrayOutputStream(streamerContext.length);
        output.write(streamerContext, 0, streamerContext.length);
        contexts.forEach((type, context) -> {
            if (nativeTypes.contains(type)) return;
            if (context.active) {
                ByteArrayOutputStream entry = new ByteArrayOutputStream();
                Proto.writeNumber(entry, 1, type);
                Proto.writeBytes(entry, 2, context.value);
                Proto.writeBytes(output, 5, entry.toByteArray());
            } else Proto.writeNumber(output, 6, type);
        });
        return output.toByteArray();
    }
}
