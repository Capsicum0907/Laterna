Nothing new in the game. Laterna now runs on more versions of it.

- **Minecraft 1.21.1** on NeoForge, as before, and now on Fabric.
- **Minecraft 1.20.1, 1.19.2, 1.18.2 and 1.16.5** on Forge and on Fabric.
- **Fabric API** is needed on Fabric. Forge and NeoForge need nothing besides the loader.
- **One jar per pair**, named `laterna-<loader>-<minecraft>-1.2.0.jar`.
- The spotlight's lit face is drawn at full brightness on NeoForge 1.21.1 and Forge 1.20.1 only. Elsewhere the loader has no way for a model to ask for it, and the face takes the light around it.
- The mod list shows the whole description. Its second sentence used to be cut off.

Every block keeps its id, its name, its texture and its recipe, so a world built on 1.1.0 opens unchanged.

---

ゲームの中身は変わっていません。動く Minecraft のバージョンが増えました。

- **Minecraft 1.21.1** は、これまでの NeoForge に加えて Fabric でも動きます。
- **Minecraft 1.20.1・1.19.2・1.18.2・1.16.5** は Forge と Fabric で動きます。
- Fabric では **Fabric API** が必要です。Forge と NeoForge はローダーのほかに何もいりません。
- **jar は組み合わせごとに1つ**で、名前は `laterna-<ローダー>-<Minecraft>-1.2.0.jar` です。
- スポットライトの光る面が最大の明るさで描かれるのは、NeoForge 1.21.1 と Forge 1.20.1 だけです。ほかではモデルからそれを指定する仕組みが無いので、周りの明るさで描かれます。
- Mod 一覧に説明文が最後まで出るようになりました。これまでは2文目が切れていました。

ブロックの id・名前・テクスチャ・レシピはどれも変わっていないので、1.1.0 で作ったワールドはそのまま開けます。
