# DressCode 项目规划

## 目标
- 基于需求确认单，交付一款类微信底部 Tab 的穿搭应用，具备登录、天气、穿搭展示/搜索/收藏、智能换装、设置等功能。
- 移动端采用 Android（Kotlin，MVVM + Room + LiveData + Navigation），后端采用 Python（FastAPI + SQLAlchemy），支持后续接入大模型。
- 兼容 Android 10 以下与 12 以上版本；代码全程 Git 管理，多次提交。

## 功能需求拆解
- 登录/注册：基础鉴权流程。
- 底部 Tab：天气、穿搭、智能换装、我的/设置。
- 天气：定位当前城市、展示天气、可切换城市。
- 穿搭展示：瀑布流列表；按性别/风格/天气/季节/场景筛选；搜索入口；收藏。
- 智能换装：上传/拍照个人照；选择收藏的穿搭提交换装任务；展示生成结果（支持重拍/替换）。
- 设置：性别设置、默认筛选；其他偏好可扩展。
- 后台/数据库：有远程服务与数据库（可先本地/云端）；数据持久化。

## Android 客户端规划
- 架构：单 Activity + Navigation；MVVM + Repository；依赖注入（Hilt/Koin）；协程 + Flow/Livedata。
- 数据层：Retrofit + OkHttp（鉴权拦截器、日志）；Room 表：users、outfits、favorites、search_history、profile_settings、weather_cache、tryon_jobs。
- UI：
  - 底部 Tab 导航。
  - 天气页：定位获取、城市切换、缓存。
  - 穿搭页：RecyclerView 瀑布流；筛选条/ChipGroup；搜索页；收藏标记。
  - 智能换装：上传/拍照（MediaStore + FileProvider）；选择收藏穿搭；提交后 WorkManager 轮询任务状态；结果展示与重试。
  - 设置页：性别与默认筛选。
- 兼容性与权限：定位/存储/相机分版本处理；scoped storage；前台定位权限（12+）；夜间模式按系统适配。
- 测试：Android 9/10 与 12/13 模拟器/真机；权限流、网络异常、缓存回退。

## Python 后端规划（FastAPI）
- 技术栈：FastAPI + Uvicorn；SQLAlchemy + Alembic；DB 优先 PostgreSQL（可先 SQLite）；对象存储本地目录，部署时接 OSS/S3；鉴权 JWT。
- 模型：User、Outfit（含 tags: style/season/weather/scene/gender）、Favorite、SearchLog、TryOnJob(status, input_user_image, outfit_ref, result_image)、CityWeatherCache、UserSetting。
- API 草案：
  - Auth：POST `/auth/register` `/auth/login`，GET `/me`
  - Outfits：GET `/outfits?gender=&style=&season=&scene=&weather=&q=`，GET `/outfits/{id}`
  - Favorites：POST `/favorites/{outfit_id}`，DELETE `/favorites/{outfit_id}`，GET `/favorites`
  - Weather：GET `/weather/current?lat=&lon=`，GET `/weather/city/{name}`
  - Try-on：POST `/tryon`（上传 user_img + outfit_id -> job_id），GET `/tryon/{job_id}`（状态/结果）
  - Settings：GET `/settings`，PATCH `/settings`
- 智能换装实现：初期可 mock（将穿搭图与人像合成/水印示意）；预留外部大模型/内部 Diffusion 服务接口；任务以队列/轮询模式返回。
- 部署：本地开发用 SQLite + 静态文件目录；生产用 PostgreSQL + 对象存储 + 反向代理；提供 .env 配置。

## 里程碑/迭代
1) 基础工程：Android 工程搭建、依赖、Navigation + BottomNav 骨架；FastAPI 骨架与数据库初始化。
2) 鉴权与设置：登录/注册、Token 存储；我的/设置页（性别与默认筛选持久化）。
3) 天气模块：定位、城市切换、API/缓存；UI 完成。
4) 穿搭模块：列表/筛选/搜索/收藏；Room 缓存；搜索历史。
5) 智能换装：上传/拍照、选择收藏穿搭、提交任务、轮询状态、展示结果（含错误重试）；初期 mock。
6) 后端完善：穿搭/收藏/搜索/天气缓存接口，换装任务队列，静态资源服务。
7) 测试与发布：多版本设备测试、权限/存储回归、性能与网络降级；准备发布构建与演示数据。

## 当前进度（前端）
- Kotlin 化工程，添加 Navigation/Lifecycle/Core-ktx 依赖，开启 viewBinding。
- 单 Activity + BottomNavigation + NavHost 骨架搭好，四个占位 Fragment（天气/穿搭/智能换装/我的）可切换，底栏采用自定义图标与主题色。
- ViewModel + 仓库完成，Fragment 通过 LiveData 订阅 UI 状态；导航切换附加动画与状态恢复配置；Profile/Login 支持后端登录/注册/退出，新增注册确认密码、重复提交防抖。
- 基本资源与字符串已填充（Tab 标题、动画、配色）；尚未接入真实数据源（穿搭/天气/换装仍为占位）。
- 引入 Hilt 并注入仓库，Activity/Fragment/ViewModel 均已标注；底栏导航动画与状态保存生效。
- 穿搭列表 UI 骨架完成（搜索卡片、筛选 Chip、瀑布流 RecyclerView 与占位数据）；收藏操作已校验登录态（未登录弹错误）；天气页添加定位/选城按钮及城市选择列表（占位数据）。
- 智能换装页支持穿搭图打标签：前端用 OkHttp 调用后端 `tag-and-suggest-name`，选图上传、标签文本化展示；未登录或未选人像/收藏穿搭会拦截提交。
- 完成度评估：页面骨架与主题就绪；数据/权限流/错误态多为占位（无 Retrofit/Room），收藏与换装未接真实后端；无多 back stack、可访问性、本地化，测试缺失。

## 当前进度（后端）
- 代码路径：`D:/MyProjects/fashion_tagging_project`，入口 `uvicorn app.main:app --host 0.0.0.0 --port 8000 --reload`（`api_server.py` 仍可兼容）。
- 结构：FastAPI + SQLAlchemy，模块化至 `app/`（routers/auth, health, tagging；services/llm_client/tagging/renaming/json_writer；db/config/security/models/schemas）。
- 鉴权：MySQL `dresscode`（root/20040129），users 表含密码哈希、访问 token + 1h 过期、refresh token + 7d 过期；接口 `/auth/register` `/auth/login` `/auth/refresh` `/auth/me`，错误码统一。
- 打标签：`POST /tag-and-suggest-name`/`/tag-image` 复用本地大模型接口（需 GEMINI_API_KEY），返回标签与建议文件名。
- 数据现状：仅 users 表入库；穿搭/收藏/换装等未落库，仍为前端占位。

## 下一步 TODO（前端优先）
- 细化主题/图标（如需设计稿适配），完善状态栏/导航栏沉浸与动画细节。
- 将仓库接入真实数据层（Retrofit+OkHttp 拦截器 + Room 持久化），保留 mock fallback；接入后端鉴权接口的 token/refresh（含失效自动刷新）与收藏/换装等业务接口。
- 登录/注册：更严格校验与账号切换数据清理；登录后刷新收藏/设置；处理多端 token 过期与 refresh。
- 权限与城市回传：封装定位权限/拒绝重试，城市选择回传；天气接口接入并补加载/空/错误态与刷新、缓存最近城市/天气。
- 穿搭模块：对接后台数据 + 分页，收藏状态持久化（Room+后端）、Chip 筛选与搜索联动，空态/错误态/重试。
- 智能换装：上传/拍照（FileProvider + 相机/存储权限链），提交任务到 mock/接口，轮询状态与结果展示，支持重拍/重试，使用收藏穿搭作为输入；标签结果用于推荐/筛选联动；切换到 HTTPS 或收敛明文白名单。
- 质量与体验：新增 ViewModel/Repository 单测与关键 UI/权限流测试；优化导航返回/多 Tab 状态，完善空态/加载骨架与可访问性/本地化；主题/动画细化。

## 目录结构建议
- Android：`app/src/main/java/.../ui/{home,weather,feed,tryon,profile}`，`data/{remote,local,repo}`，`domain/models`，`common`。
- Backend：`backend/main.py`，`backend/app/routers/{auth,outfits,favorites,weather,tryon,settings}`，`backend/app/{models,schemas,services}`，`alembic/`。

## 验收与操作要点
- Git 持续提交并截图记录。
- Android 10 以下与 12 以上测试通过；权限流顺畅。
- 智能换装接口可在无大模型时提供 mock 结果；后续可替换为真实推理服务。
- 天气与穿搭数据可先用 mock/静态资源，逐步切换为真实数据源。
