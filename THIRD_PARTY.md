# Third-party notices and binary releases

Glimmer Qt was extracted from [Snitt](https://github.com/isaksky/snitt). Its
original code remains under the [MIT license](LICENSE), including the existing
Snitt contributor attribution. Dependencies and third-party assets retain their
own licenses.

## Material included in this repository

| Material | License and provenance |
| --- | --- |
| `vendor/glimmer/` | MIT, copyright 2026 Dmitri Sotnikov. Preserve [its license](vendor/glimmer/LICENSE). This is a modified copy of upstream revision `09b8ab08705fa1e6f20145c2e99eaaba9ea812cc`; the changes are described in [LIFECYCLE.md](LIFECYCLE.md#explicit-glimmer-contract-extension). |
| `test/icons/scissors.svg` | Lucide, ISC, copied from Snitt's `src/icons/scissors.svg`. Preserve [the icon license](test/icons/LICENSE), copied intact from Snitt, including the upstream Feather notice for the icons to which it applies. |

Include these notices when distributing the corresponding material, including
source archives and test fixtures. The complete Lucide notice is retained for
provenance; its Feather section names the icons covered by that additional notice.

## Developer notice: before distributing binaries

`build-native` builds a shared bridge against an installed Qt SDK. It does not
assemble a release package or collect dependency notices and source materials.
The root MIT license alone does not cover a binary distribution's dependencies.
Review both standalone bridge releases and applications that include the bridge.

1. **Inventory the actual release.** Record versions, source revisions, build
   options, and licenses for the Qt libraries, QML modules, plugins, multimedia
   backend, and transitive libraries included in the artifact. Include Jolt and
   its runtime dependencies if bundled. Check the selected versions and modules;
   some Qt modules are GPL-only under their open-source terms. Qt SBOMs can help
   identify components. See [Qt licensing](https://doc.qt.io/qt-6/licensing.html).

2. **Include the required notices and license texts.** Preserve the notices
   above for material shipped. For Qt under LGPLv3, include a prominent Qt usage
   notice and copies of both LGPLv3 and GPLv3. Include the notices of Qt's own
   third-party components and other bundled dependencies. A commercial Qt
   license has different Qt terms but does not replace third-party licenses.

3. **Provide the required corresponding source.** When shipping LGPL libraries,
   fulfill their source-distribution obligations for the exact binaries shipped,
   including modifications and required build material. Publishing matching
   source archives alongside release downloads is a practical approach. Keep
   source checksums, patches, and configuration/build instructions with the
   release records. A link to an upstream project's home page is insufficient.

4. **Preserve library replacement.** For the LGPL shared-library route, users
   must be able to use compatible modified libraries. Verify that packaging and
   signing permit this, document replacement and any necessary local signing
   steps, and supply installation information where required. Do not prohibit
   reverse engineering needed to debug library modifications. Static linking
   needs a separate relinking/source plan. Dynamic linking alone does not meet
   every obligation. See [LGPLv3, section 4](https://doc.qt.io/qt-6/lgpl.html).

5. **Check the FFmpeg build separately.** Its license depends on enabled
   components. For an LGPL-only playback runtime, avoid `--enable-gpl`,
   `--enable-nonfree`, and GPL dependencies such as libx264; retain the exact
   configuration and corresponding source. A developer's installed FFmpeg is
   not automatically suitable for redistribution. See
   [FFmpeg's licensing guidance](https://ffmpeg.org/legal.html).

`test-media` invokes a local `ffmpeg` with libx264 to generate a temporary test
clip. That test tool is separate from the FFmpeg libraries loaded by Qt's
multimedia plugin; inspect the plugin's actual dependencies for a release.

Snitt's [experiment packaging notes](https://github.com/isaksky/snitt/blob/91f347c5b09514604ffcf71d972d5e90cf42c37a/experiment/PACKAGING.md)
and [packaging script](https://github.com/isaksky/snitt/blob/91f347c5b09514604ffcf71d972d5e90cf42c37a/bin/package-glimmer-macos)
are implementation references for collecting notices, a dependency inventory,
and source/build materials. That packaging workflow was not extracted into this
repository. This notice is developer guidance; each release still needs its own
inventory and license materials.
