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
- 单 Activity + BottomNavigation + NavHost 骨架搭好，四个占位 Fragment（天气/穿搭/智能换装/我的）可切换。
- 基本资源与字符串已填充（Tab 标题与临时图标），尚未接入实际数据或 ViewModel。
- 尚未运行构建/测试，可能需联网下载依赖。

## 下一步 TODO（前端优先）
- 替换 BottomNav 图标与主题配色，确定视觉基调。
- 为各 Fragment 增加 ViewModel/Repository 框架与导航动画占位，建立基础数据模型。
- 接入登录/注册入口与路由（决定是启动页或我的页内触发）。
- 天气模块：权限请求流程草稿 + 城市选择页框架。
- 穿搭模块：RecyclerView 瀑布流骨架 + 筛选条/搜索入口占位。
- 智能换装：上传/拍照流程设计稿及权限处理方案草稿。

## 目录结构建议
- Android：`app/src/main/java/.../ui/{home,weather,feed,tryon,profile}`，`data/{remote,local,repo}`，`domain/models`，`common`。
- Backend：`backend/main.py`，`backend/app/routers/{auth,outfits,favorites,weather,tryon,settings}`，`backend/app/{models,schemas,services}`，`alembic/`。

## 验收与操作要点
- Git 持续提交并截图记录。
- Android 10 以下与 12 以上测试通过；权限流顺畅。
- 智能换装接口可在无大模型时提供 mock 结果；后续可替换为真实推理服务。
- 天气与穿搭数据可先用 mock/静态资源，逐步切换为真实数据源。
