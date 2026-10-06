// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-FileCopyrightText: 2026 ReVanced contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.youtube.litho;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import app.reseam.youtube.litho.FilterGroup.ByteArrayFilterGroup;
import app.reseam.youtube.litho.FilterGroup.StringFilterGroup;

/**
 * Hides litho components.
 * <p>
 * The patch that ships a subclass hands an instance to {@link LithoFilter#register} at process
 * start, and the subclass registers what it wants to see through {@link #addIdentifierCallbacks}
 * and {@link #addPathCallbacks} from its constructor. All callbacks must be registered before the
 * constructor completes.
 * <p>
 * To filter on {@link FilterContentType#PROTOBUFFER} or {@link FilterContentType#ACCESSIBILITY},
 * first register an identifier or path callback, then search the buffer inside
 * {@link #isFiltered} with a {@link ByteArrayFilterGroup} for one pattern or a
 * {@link FilterGroupList.ByteArrayFilterGroupList} for several.
 */
public abstract class Filter {

    public enum FilterContentType {
        IDENTIFIER,
        PATH,
        ACCESSIBILITY,
        PROTOBUFFER
    }

    /** Do not add to this directly; use {@link #addIdentifierCallbacks}. */
    public final List<StringFilterGroup> identifierCallbacks = new ArrayList<>();

    /** Do not add to this directly; use {@link #addPathCallbacks}. */
    public final List<StringFilterGroup> pathCallbacks = new ArrayList<>();

    protected final void addIdentifierCallbacks(StringFilterGroup... groups) {
        identifierCallbacks.addAll(Arrays.asList(groups));
    }

    protected final void addPathCallbacks(StringFilterGroup... groups) {
        pathCallbacks.addAll(Arrays.asList(groups));
    }

    /**
     * Called off the main thread after an enabled filter matched. The default is to hide the
     * matched component; a subclass overrides to check the buffer or the accessibility text.
     *
     * @param identifier    Litho identifier.
     * @param accessibility Accessibility string, or an empty string when the component has none.
     * @param buffer        Protocol buffer.
     * @param matchedGroup  The filter that matched.
     * @param contentType   The type of content matched.
     * @param contentIndex  Matched index within the identifier or path.
     * @return True if the litho component should be hidden.
     */
    public boolean isFiltered(String identifier, String accessibility, String path, byte[] buffer,
                              StringFilterGroup matchedGroup, FilterContentType contentType, int contentIndex) {
        return true;
    }
}
