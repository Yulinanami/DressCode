 # DressCode · Android 客户端 

  Android + FastAPI 的穿搭 / 智能换装应用，前端采用 Kotlin MVVM 架构，
  后端采用 Python FastAPI，支持穿搭浏览、筛选、收藏、上传打标签、天气推
  荐和智能换装功能。

  - 本仓库：Android 客户端
    https://github.com/Yulinanami/DressCode
  - 后端服务仓库：
    https://github.com/Yulinanami/fashion_tagging_project

  注意：需要 **同时** 启动本仓库（Android App）和后端仓库，并在前端正确配置后端
  地址。

  ## 功能概览

  - 穿搭列表：
    - 瀑布流列表 + Paging 分页加载。
    - 按风格 / 场景 / 天气 / 季节筛选，搜索关键字。
    - 收藏 / 取消收藏，以及“我的收藏”列表。
    - 支持上传本地图片，由后端打标签入库，列表可区分自上传穿搭并支持
  删除。
  - 天气：
    - 展示当前城市天气、温度信息。
    - 支持城市切换、定）。
    - 基于当前天气从后端获取推荐穿搭，并一键送入智能换装。
  - 智能换装：
    - 上传 / 选择人像和衣物两张图片，发起后端换装任务。
    - 可直接使用“天气推荐”的穿搭作为衣物输入，一键换装。


  ## 本地运行（Android 客户端）

  1. 克隆项目到本地，并使用 Android Studio 打开：

     git clone https://github.com/Yulinanami/DressCode.git
  2. 配置后端地址：
        API_BASE_URL=http://10.0.2.2:8000
        TAGGING_BASE_URL=http://10.0.2.2:8000

  注意：使用模拟器访问宿主机上的 FastAPI 服务，应使用 http://10.0.2.2:8000。
  - 使用真机时，需要将后端服务部署在同一局域网中，并把 API_BASE_URL /
    TAGGING_BASE_URL 配置为 http://<电脑局域网IP>:8000。

