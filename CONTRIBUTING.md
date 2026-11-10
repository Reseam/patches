# Contributing

Every contributor agrees to these terms before their first contribution is merged.

## License

This repository is licensed under the GNU Affero General Public License, version 3 or later, with the additional terms in [NOTICE](NOTICE) under section 7 of that license.

When you contribute to this repository, you license your contribution under those same terms. You authorize the NOTICE terms for it under section 7, and you confirm that you have the right to license it this way.

Code you did not write keeps its original copyright and license. Name its source and license when you add it.

## Copyright headers

Files that carry a copyright header use the project's collective notice:

    SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
    SPDX-License-Identifier: AGPL-3.0-or-later

The commit history records who wrote what. A file that names another copyright holder falls outside the NOTICE terms, so new files use this header unchanged.

## Writing strings

Every string users see lives in a `src/main/strings/values/strings.xml`: settings text in each `settings` module, patch names, descriptions and option text in each `patch` module. Translators work from the English text, so keep it plain and consistent across apps.

- Patch and setting titles are in sentence case and start with the verb when they do something: Hide for parts of the app, Block for ads, trackers and requests, Disable for behaviors, Unlock for paid features. A feature that exists in several apps has the same name in each.
- A patch description is one sentence in the third person, such as "Hides promoted posts in feeds and search." Name any third party the patch sends data to, and what it sees.
- A setting's summary is optional. Add one only when it says something the title doesn't: where the setting applies, a side effect, or a privacy trade-off. Never repeat the title.
- Don't tell users to restart the app, and don't mention SD cards.
- A count is a `<plurals>` with an integer `count` argument, never "1 items". Every value in a sentence is an `xliff:g` argument, so translators can move it.
- Mark text that reads the same in every language, such as host names and product names, with `translatable="false"`.

## Translations

Translations come from Weblate and land as `values-<locale>/strings.xml` next to each `values` folder. Don't edit them by hand, since Weblate overwrites them. A translation can leave strings out, and those show in English. The build fails on a translation whose arguments differ from the English text, or that translates a string marked `translatable="false"`.
