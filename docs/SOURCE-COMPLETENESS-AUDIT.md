# 源码完整性核对

交付版本：1.0.20261031.6（版本号，不是文件大小）。

原始 ZIP SHA-256：`c504ae00bc37193f531c04dcd699eaeed76d1e787159c4a626291adc44ad59ee`

原始源码 668 个文件，交付 695 个文件，原始文件缺失 **0** 个。

本版按用户要求以提供的 PNG 替换此前额外新增的机器人 SVG，旧 SVG 不再交付；并未删除任何原始上传的源码文件。

新增文件：

- `android/app/src/main/java/com/github/manager/ApkProvenanceVerifier.kt`
- `android/app/src/main/java/com/github/manager/FileChooserPolicy.kt`
- `android/app/src/test/java/com/github/manager/FileChooserPolicyTest.java`
- `docs/BATCH-MATCH-AND-PROFILE-FIX.md`
- `docs/LIBRARY-LAYOUT-V6.md`
- `docs/PROVENANCE-AND-RECOVERY.md`
- `docs/SOURCE-COMPLETENESS-AUDIT.md`
- `scripts/build-app-source-index.py`
- `src/assets/chatgpt-helper.png`
- `src/components/ProvenancePanel.tsx`
- `src/components/common/RepositoryReadme.tsx`
- `src/components/common/SectionBoundary.tsx`
- `src/data/app-source-index.json`
- `src/lib/compactSources.ts`
- `src/lib/markdownLinks.ts`
- `src/lib/packageSources.ts`
- `src/lib/provenance.ts`
- `src/services/appProvenance.ts`
- `src/tests/appLibrary.batch.test.tsx`
- `src/tests/appProvenance.flow.test.ts`
- `src/tests/compactSources.test.ts`
- `src/tests/contributorCredits.test.tsx`
- `src/tests/markdownLinks.test.tsx`
- `src/tests/packageSources.test.ts`
- `src/tests/provenance.test.ts`
- `src/tests/sectionRecovery.test.tsx`
- `src/tests/technicalTranslation.test.ts`

修改的原文件：

- `android/app/build.gradle.kts`
- `android/app/src/main/java/com/github/manager/InstalledAppsScanner.kt`
- `android/app/src/main/java/com/github/manager/MainActivity.kt`
- `package.json`
- `pnpm-lock.yaml`
- `src/components/common/MarkdownRenderer.tsx`
- `src/components/settings/ContributorsSection.tsx`
- `src/lib/apkEvidence.ts`
- `src/lib/appLibrary.ts`
- `src/lib/auto-translator.ts`
- `src/lib/installedApps.ts`
- `src/pages/AppLibraryPage.tsx`
- `src/pages/RepoDetailPage.tsx`
- `src/services/github.ts`
- `src/services/publicProxy.ts`

验证：159 项前端测试、4 项基于实际编译 Kotlin 类的 JUnit 文件选择规则测试、TypeScript、Web 生产构建和 Android Release 构建通过。APK 签名复用上一版。头像与用户提供 PNG 逐字节相同；牡丹君仅保留一条、只显示作者主页链接。

node_modules、SDK、编译缓存和私有签名不属于源码包。未宣称完成真机视觉验收。
