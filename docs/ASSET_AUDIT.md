# Asset audit

Q015 found no unused bitmap or font asset. Each avatar is referenced by the avatar catalog and UI,
and every bundled font is referenced by the Compose theme or notification layouts.

The six PNG avatars are already 146–154 bytes each, so recompression would not materially change
the APK. The retained font files are required for the Comic, Pixel, and Quran reading experiences.
Future image additions should use an Android density bucket (or `drawable-nodpi` when intentional)
and be measured against the APK size report before adding variants.
