
# TestSprite AI Testing Report(MCP)

---

## 1️⃣ Document Metadata
- **Project Name:** itda-frontend
- **Date:** 2026-01-23
- **Prepared by:** TestSprite AI Team

---

## 2️⃣ Requirement Validation Summary

#### Test TC001 User login success with valid credentials
- **Test Code:** [TC001_User_login_success_with_valid_credentials.py](./TC001_User_login_success_with_valid_credentials.py)
- **Test Error:** Login attempt with valid credentials failed as the form resets without success or error message. The login functionality is not working as expected. Reporting this issue and stopping further testing.
- **Test Visualization and Result:** https://www.testsprite.com/dashboard/mcp/tests/b8d10419-9992-4aee-9c91-f1d098d8c71b/32531ff8-13cc-4206-b72f-a355d5b6d55b
- **Status:** ❌ Failed
- **Analysis / Findings:** {{TODO:AI_ANALYSIS}}.
---

#### Test TC002 User registration success with valid data
- **Test Code:** [TC002_User_registration_success_with_valid_data.py](./TC002_User_registration_success_with_valid_data.py)
- **Test Visualization and Result:** https://www.testsprite.com/dashboard/mcp/tests/b8d10419-9992-4aee-9c91-f1d098d8c71b/90449291-79da-4785-81a6-194f50ef8b9d
- **Status:** ✅ Passed
- **Analysis / Findings:** {{TODO:AI_ANALYSIS}}.
---

#### Test TC003 Dashboard loads and displays user projects
- **Test Code:** [TC003_Dashboard_loads_and_displays_user_projects.py](./TC003_Dashboard_loads_and_displays_user_projects.py)
- **Test Error:** Test stopped due to UI issue: Clicking a project item opens the 'New Project' modal instead of navigating to the project detail page. Login and dashboard loading with projects list are confirmed, but project navigation cannot be verified.
- **Test Visualization and Result:** https://www.testsprite.com/dashboard/mcp/tests/b8d10419-9992-4aee-9c91-f1d098d8c71b/d180894f-8f79-48fb-9dc4-2d8aff73fb1a
- **Status:** ❌ Failed
- **Analysis / Findings:** {{TODO:AI_ANALYSIS}}.
---

#### Test TC004 Project detail page loads project and scenes with loading indication
- **Test Code:** [TC004_Project_detail_page_loads_project_and_scenes_with_loading_indication.py](./TC004_Project_detail_page_loads_project_and_scenes_with_loading_indication.py)
- **Test Visualization and Result:** https://www.testsprite.com/dashboard/mcp/tests/b8d10419-9992-4aee-9c91-f1d098d8c71b/28ba9763-c02c-48c4-b498-947987ee5a8d
- **Status:** ✅ Passed
- **Analysis / Findings:** {{TODO:AI_ANALYSIS}}.
---

#### Test TC005 Error handling when nodeStore.loadSceneNodes API fails
- **Test Code:** [TC005_Error_handling_when_nodeStore.loadSceneNodes_API_fails.py](./TC005_Error_handling_when_nodeStore.loadSceneNodes_API_fails.py)
- **Test Visualization and Result:** https://www.testsprite.com/dashboard/mcp/tests/b8d10419-9992-4aee-9c91-f1d098d8c71b/2bc3b94a-4fd2-4601-a33b-dbabea901add
- **Status:** ✅ Passed
- **Analysis / Findings:** {{TODO:AI_ANALYSIS}}.
---

#### Test TC006 URL parameters projectId and sceneId parsed correctly on page load
- **Test Code:** [TC006_URL_parameters_projectId_and_sceneId_parsed_correctly_on_page_load.py](./TC006_URL_parameters_projectId_and_sceneId_parsed_correctly_on_page_load.py)
- **Test Visualization and Result:** https://www.testsprite.com/dashboard/mcp/tests/b8d10419-9992-4aee-9c91-f1d098d8c71b/c80b634f-bbb4-40bf-b0cd-0f0e2e573c82
- **Status:** ✅ Passed
- **Analysis / Findings:** {{TODO:AI_ANALYSIS}}.
---

#### Test TC007 Node selection and deselection updates states properly
- **Test Code:** [TC007_Node_selection_and_deselection_updates_states_properly.py](./TC007_Node_selection_and_deselection_updates_states_properly.py)
- **Test Visualization and Result:** https://www.testsprite.com/dashboard/mcp/tests/b8d10419-9992-4aee-9c91-f1d098d8c71b/054fb902-bd40-4250-8e07-4ca67806d0dc
- **Status:** ✅ Passed
- **Analysis / Findings:** {{TODO:AI_ANALYSIS}}.
---

#### Test TC008 Keyboard shortcuts Delete and Backspace delete nodes only when input not focused
- **Test Code:** [TC008_Keyboard_shortcuts_Delete_and_Backspace_delete_nodes_only_when_input_not_focused.py](./TC008_Keyboard_shortcuts_Delete_and_Backspace_delete_nodes_only_when_input_not_focused.py)
- **Test Visualization and Result:** https://www.testsprite.com/dashboard/mcp/tests/b8d10419-9992-4aee-9c91-f1d098d8c71b/8e0ff6a9-4e52-4216-91d2-81cc06631098
- **Status:** ✅ Passed
- **Analysis / Findings:** {{TODO:AI_ANALYSIS}}.
---

#### Test TC009 Ctrl+Z undo reverts last node editing action properly
- **Test Code:** [TC009_CtrlZ_undo_reverts_last_node_editing_action_properly.py](./TC009_CtrlZ_undo_reverts_last_node_editing_action_properly.py)
- **Test Error:** Tested modifying a node by adding '그리드 1' and attempted to undo the change using Ctrl+Z twice. The node remained present, indicating the undo functionality is not working as intended. This is a critical issue for the scene editor's node modification history feature.
Browser Console Logs:
[WARNING] [Vue warn]: Failed to resolve component: Users
If this is a native custom element, make sure to exclude it from component resolution via compilerOptions.isCustomElement. 
  at <ProjectLayout project=null active-tab="story" scene-count=0  ... > 
  at <ProjectDetailPage onVnodeUnmounted=fn<onVnodeUnmounted> ref=Ref< null > > 
  at <RouterView> 
  at <App> (at http://localhost:5173/node_modules/.vite/deps/chunk-2MKFL3BX.js?v=fcff0cfd:2194:12)
[WARNING] [Vue warn]: Failed to resolve component: Users
If this is a native custom element, make sure to exclude it from component resolution via compilerOptions.isCustomElement. 
  at <ProjectLayout project= {projectId: 1, title: The Martian Red, description: A survival story on Mars. The protagonist finds an ancient ruin., genre: Sci-Fi, thumbnailUrl: https://images.unsplash.com/photo-1626814026160-2237a95fc5a0?w=600&auto=format} active-tab="story" scene-count=0  ... > 
  at <ProjectDetailPage onVnodeUnmounted=fn<onVnodeUnmounted> ref=Ref< Proxy(Object) > > 
  at <RouterView> 
  at <App> (at http://localhost:5173/node_modules/.vite/deps/chunk-2MKFL3BX.js?v=fcff0cfd:2194:12)
[WARNING] [Vue warn]: Failed to resolve component: Users
If this is a native custom element, make sure to exclude it from component resolution via compilerOptions.isCustomElement. 
  at <ProjectLayout project= {projectId: 1, title: The Martian Red, description: A survival story on Mars. The protagonist finds an ancient ruin., genre: Sci-Fi, thumbnailUrl: https://images.unsplash.com/photo-1626814026160-2237a95fc5a0?w=600&auto=format} active-tab="story" scene-count=3  ... > 
  at <ProjectDetailPage onVnodeUnmounted=fn<onVnodeUnmounted> ref=Ref< Proxy(Object) > > 
  at <RouterView> 
  at <App> (at http://localhost:5173/node_modules/.vite/deps/chunk-2MKFL3BX.js?v=fcff0cfd:2194:12)
[WARNING] [Vue warn]: Failed to resolve component: Users
If this is a native custom element, make sure to exclude it from component resolution via compilerOptions.isCustomElement. 
  at <ProjectLayout project= {projectId: 1, title: The Martian Red, description: A survival story on Mars. The protagonist finds an ancient ruin., genre: Sci-Fi, thumbnailUrl: https://images.unsplash.com/photo-1626814026160-2237a95fc5a0?w=600&auto=format} active-tab="story" scene-count=3  ... > 
  at <ProjectDetailPage onVnodeUnmounted=fn<onVnodeUnmounted> ref=Ref< Proxy(Object) > > 
  at <RouterView> 
  at <App> (at http://localhost:5173/node_modules/.vite/deps/chunk-2MKFL3BX.js?v=fcff0cfd:2194:12)
[WARNING] [Vue warn]: Failed to resolve component: Users
If this is a native custom element, make sure to exclude it from component resolution via compilerOptions.isCustomElement. 
  at <ProjectLayout project=null active-tab="story" scene-count=0  ... > 
  at <ProjectDetailPage onVnodeUnmounted=fn<onVnodeUnmounted> ref=Ref< undefined > > 
  at <RouterView> 
  at <App> (at http://localhost:5173/node_modules/.vite/deps/chunk-2MKFL3BX.js?v=fcff0cfd:2194:12)
[WARNING] [Vue warn]: Failed to resolve component: Users
If this is a native custom element, make sure to exclude it from component resolution via compilerOptions.isCustomElement. 
  at <ProjectLayout project= {projectId: 1, title: The Martian Red, description: A survival story on Mars. The protagonist finds an ancient ruin., genre: Sci-Fi, thumbnailUrl: https://images.unsplash.com/photo-1626814026160-2237a95fc5a0?w=600&auto=format} active-tab="story" scene-count=0  ... > 
  at <ProjectDetailPage onVnodeUnmounted=fn<onVnodeUnmounted> ref=Ref< Proxy(Object) > > 
  at <RouterView> 
  at <App> (at http://localhost:5173/node_modules/.vite/deps/chunk-2MKFL3BX.js?v=fcff0cfd:2194:12)
[WARNING] [Vue warn]: Failed to resolve component: Users
If this is a native custom element, make sure to exclude it from component resolution via compilerOptions.isCustomElement. 
  at <ProjectLayout project= {projectId: 1, title: The Martian Red, description: A survival story on Mars. The protagonist finds an ancient ruin., genre: Sci-Fi, thumbnailUrl: https://images.unsplash.com/photo-1626814026160-2237a95fc5a0?w=600&auto=format} active-tab="story" scene-count=3  ... > 
  at <ProjectDetailPage onVnodeUnmounted=fn<onVnodeUnmounted> ref=Ref< Proxy(Object) > > 
  at <RouterView> 
  at <App> (at http://localhost:5173/node_modules/.vite/deps/chunk-2MKFL3BX.js?v=fcff0cfd:2194:12)
[WARNING] [Vue warn]: Failed to resolve component: Users
If this is a native custom element, make sure to exclude it from component resolution via compilerOptions.isCustomElement. 
  at <ProjectLayout project= {projectId: 1, title: The Martian Red, description: A survival story on Mars. The protagonist finds an ancient ruin., genre: Sci-Fi, thumbnailUrl: https://images.unsplash.com/photo-1626814026160-2237a95fc5a0?w=600&auto=format} active-tab="story" scene-count=3  ... > 
  at <ProjectDetailPage onVnodeUnmounted=fn<onVnodeUnmounted> ref=Ref< Proxy(Object) > > 
  at <RouterView> 
  at <App> (at http://localhost:5173/node_modules/.vite/deps/chunk-2MKFL3BX.js?v=fcff0cfd:2194:12)
- **Test Visualization and Result:** https://www.testsprite.com/dashboard/mcp/tests/b8d10419-9992-4aee-9c91-f1d098d8c71b/9a70e5fb-0c18-4bb5-ba4b-be13019f01dd
- **Status:** ❌ Failed
- **Analysis / Findings:** {{TODO:AI_ANALYSIS}}.
---

#### Test TC010 Auto-layout rearranges nodes correctly without layout corruption
- **Test Code:** [TC010_Auto_layout_rearranges_nodes_correctly_without_layout_corruption.py](./TC010_Auto_layout_rearranges_nodes_correctly_without_layout_corruption.py)
- **Test Error:** Test stopped due to UI bug: The auto-layout feature cannot be accessed because clicking the Settings button leads to a blank page with no layout options. Nodes were added in disorder but auto-layout invocation failed. No links or graph connections could be verified. Please fix the issue to enable further testing.
Browser Console Logs:
[WARNING] [Vue warn]: Failed to resolve component: Users
If this is a native custom element, make sure to exclude it from component resolution via compilerOptions.isCustomElement. 
  at <ProjectLayout project=null active-tab="story" scene-count=0  ... > 
  at <ProjectDetailPage onVnodeUnmounted=fn<onVnodeUnmounted> ref=Ref< null > > 
  at <RouterView> 
  at <App> (at http://localhost:5173/node_modules/.vite/deps/chunk-2MKFL3BX.js?v=fcff0cfd:2194:12)
[WARNING] [Vue warn]: Failed to resolve component: Users
If this is a native custom element, make sure to exclude it from component resolution via compilerOptions.isCustomElement. 
  at <ProjectLayout project= {projectId: 1, title: The Martian Red, description: A survival story on Mars. The protagonist finds an ancient ruin., genre: Sci-Fi, thumbnailUrl: https://images.unsplash.com/photo-1626814026160-2237a95fc5a0?w=600&auto=format} active-tab="story" scene-count=0  ... > 
  at <ProjectDetailPage onVnodeUnmounted=fn<onVnodeUnmounted> ref=Ref< Proxy(Object) > > 
  at <RouterView> 
  at <App> (at http://localhost:5173/node_modules/.vite/deps/chunk-2MKFL3BX.js?v=fcff0cfd:2194:12)
[WARNING] [Vue warn]: Failed to resolve component: Users
If this is a native custom element, make sure to exclude it from component resolution via compilerOptions.isCustomElement. 
  at <ProjectLayout project= {projectId: 1, title: The Martian Red, description: A survival story on Mars. The protagonist finds an ancient ruin., genre: Sci-Fi, thumbnailUrl: https://images.unsplash.com/photo-1626814026160-2237a95fc5a0?w=600&auto=format} active-tab="story" scene-count=3  ... > 
  at <ProjectDetailPage onVnodeUnmounted=fn<onVnodeUnmounted> ref=Ref< Proxy(Object) > > 
  at <RouterView> 
  at <App> (at http://localhost:5173/node_modules/.vite/deps/chunk-2MKFL3BX.js?v=fcff0cfd:2194:12)
[WARNING] [Vue warn]: Failed to resolve component: Users
If this is a native custom element, make sure to exclude it from component resolution via compilerOptions.isCustomElement. 
  at <ProjectLayout project= {projectId: 1, title: The Martian Red, description: A survival story on Mars. The protagonist finds an ancient ruin., genre: Sci-Fi, thumbnailUrl: https://images.unsplash.com/photo-1626814026160-2237a95fc5a0?w=600&auto=format} active-tab="story" scene-count=3  ... > 
  at <ProjectDetailPage onVnodeUnmounted=fn<onVnodeUnmounted> ref=Ref< Proxy(Object) > > 
  at <RouterView> 
  at <App> (at http://localhost:5173/node_modules/.vite/deps/chunk-2MKFL3BX.js?v=fcff0cfd:2194:12)
[WARNING] [Vue warn]: Failed to resolve component: Users
If this is a native custom element, make sure to exclude it from component resolution via compilerOptions.isCustomElement. 
  at <ProjectLayout project=null active-tab="story" scene-count=0  ... > 
  at <ProjectDetailPage onVnodeUnmounted=fn<onVnodeUnmounted> ref=Ref< undefined > > 
  at <RouterView> 
  at <App> (at http://localhost:5173/node_modules/.vite/deps/chunk-2MKFL3BX.js?v=fcff0cfd:2194:12)
[WARNING] [Vue warn]: Failed to resolve component: Users
If this is a native custom element, make sure to exclude it from component resolution via compilerOptions.isCustomElement. 
  at <ProjectLayout project= {projectId: 1, title: The Martian Red, description: A survival story on Mars. The protagonist finds an ancient ruin., genre: Sci-Fi, thumbnailUrl: https://images.unsplash.com/photo-1626814026160-2237a95fc5a0?w=600&auto=format} active-tab="story" scene-count=0  ... > 
  at <ProjectDetailPage onVnodeUnmounted=fn<onVnodeUnmounted> ref=Ref< Proxy(Object) > > 
  at <RouterView> 
  at <App> (at http://localhost:5173/node_modules/.vite/deps/chunk-2MKFL3BX.js?v=fcff0cfd:2194:12)
[WARNING] [Vue warn]: Failed to resolve component: Users
If this is a native custom element, make sure to exclude it from component resolution via compilerOptions.isCustomElement. 
  at <ProjectLayout project= {projectId: 1, title: The Martian Red, description: A survival story on Mars. The protagonist finds an ancient ruin., genre: Sci-Fi, thumbnailUrl: https://images.unsplash.com/photo-1626814026160-2237a95fc5a0?w=600&auto=format} active-tab="story" scene-count=3  ... > 
  at <ProjectDetailPage onVnodeUnmounted=fn<onVnodeUnmounted> ref=Ref< Proxy(Object) > > 
  at <RouterView> 
  at <App> (at http://localhost:5173/node_modules/.vite/deps/chunk-2MKFL3BX.js?v=fcff0cfd:2194:12)
[WARNING] [Vue warn]: Failed to resolve component: Users
If this is a native custom element, make sure to exclude it from component resolution via compilerOptions.isCustomElement. 
  at <ProjectLayout project= {projectId: 1, title: The Martian Red, description: A survival story on Mars. The protagonist finds an ancient ruin., genre: Sci-Fi, thumbnailUrl: https://images.unsplash.com/photo-1626814026160-2237a95fc5a0?w=600&auto=format} active-tab="story" scene-count=3  ... > 
  at <ProjectDetailPage onVnodeUnmounted=fn<onVnodeUnmounted> ref=Ref< Proxy(Object) > > 
  at <RouterView> 
  at <App> (at http://localhost:5173/node_modules/.vite/deps/chunk-2MKFL3BX.js?v=fcff0cfd:2194:12)
[WARNING] [Vue warn]: Failed to resolve component: Users
If this is a native custom element, make sure to exclude it from component resolution via compilerOptions.isCustomElement. 
  at <ProjectLayout project= {projectId: 1, title: The Martian Red, description: A survival story on Mars. The protagonist finds an ancient ruin., genre: Sci-Fi, thumbnailUrl: https://images.unsplash.com/photo-1626814026160-2237a95fc5a0?w=600&auto=format} active-tab="story" scene-count=4  ... > 
  at <ProjectDetailPage onVnodeUnmounted=fn<onVnodeUnmounted> ref=Ref< Proxy(Object) > > 
  at <RouterView> 
  at <App> (at http://localhost:5173/node_modules/.vite/deps/chunk-2MKFL3BX.js?v=fcff0cfd:2194:12)
[WARNING] [Vue warn]: Failed to resolve component: Users
If this is a native custom element, make sure to exclude it from component resolution via compilerOptions.isCustomElement. 
  at <ProjectLayout project= {projectId: 1, title: The Martian Red, description: A survival story on Mars. The protagonist finds an ancient ruin., genre: Sci-Fi, thumbnailUrl: https://images.unsplash.com/photo-1626814026160-2237a95fc5a0?w=600&auto=format} active-tab="story" scene-count=5  ... > 
  at <ProjectDetailPage onVnodeUnmounted=fn<onVnodeUnmounted> ref=Ref< Proxy(Object) > > 
  at <RouterView> 
  at <App> (at http://localhost:5173/node_modules/.vite/deps/chunk-2MKFL3BX.js?v=fcff0cfd:2194:12)
[WARNING] [Vue warn]: Failed to resolve component: Users
If this is a native custom element, make sure to exclude it from component resolution via compilerOptions.isCustomElement. 
  at <ProjectLayout project= {projectId: 1, title: The Martian Red, description: A survival story on Mars. The protagonist finds an ancient ruin., genre: Sci-Fi, thumbnailUrl: https://images.unsplash.com/photo-1626814026160-2237a95fc5a0?w=600&auto=format} active-tab="story" scene-count=5  ... > 
  at <ProjectDetailPage onVnodeUnmounted=fn<onVnodeUnmounted> ref=Ref< Proxy(Object) > > 
  at <RouterView> 
  at <App> (at http://localhost:5173/node_modules/.vite/deps/chunk-2MKFL3BX.js?v=fcff0cfd:2194:12)
[WARNING] [Vue warn]: Failed to resolve component: Users
If this is a native custom element, make sure to exclude it from component resolution via compilerOptions.isCustomElement. 
  at <ProjectLayout project=null active-tab="story" scene-count=0  ... > 
  at <ProjectDetailPage onVnodeUnmounted=fn<onVnodeUnmounted> ref=Ref< undefined > > 
  at <RouterView> 
  at <App> (at http://localhost:5173/node_modules/.vite/deps/chunk-2MKFL3BX.js?v=fcff0cfd:2194:12)
[WARNING] [Vue warn]: Failed to resolve component: Users
If this is a native custom element, make sure to exclude it from component resolution via compilerOptions.isCustomElement. 
  at <ProjectLayout project= {projectId: 1, title: The Martian Red, description: A survival story on Mars. The protagonist finds an ancient ruin., genre: Sci-Fi, thumbnailUrl: https://images.unsplash.com/photo-1626814026160-2237a95fc5a0?w=600&auto=format} active-tab="story" scene-count=0  ... > 
  at <ProjectDetailPage onVnodeUnmounted=fn<onVnodeUnmounted> ref=Ref< Proxy(Object) > > 
  at <RouterView> 
  at <App> (at http://localhost:5173/node_modules/.vite/deps/chunk-2MKFL3BX.js?v=fcff0cfd:2194:12)
[WARNING] [Vue warn]: Failed to resolve component: Users
If this is a native custom element, make sure to exclude it from component resolution via compilerOptions.isCustomElement. 
  at <ProjectLayout project= {projectId: 1, title: The Martian Red, description: A survival story on Mars. The protagonist finds an ancient ruin., genre: Sci-Fi, thumbnailUrl: https://images.unsplash.com/photo-1626814026160-2237a95fc5a0?w=600&auto=format} active-tab="story" scene-count=3  ... > 
  at <ProjectDetailPage onVnodeUnmounted=fn<onVnodeUnmounted> ref=Ref< Proxy(Object) > > 
  at <RouterView> 
  at <App> (at http://localhost:5173/node_modules/.vite/deps/chunk-2MKFL3BX.js?v=fcff0cfd:2194:12)
[WARNING] [Vue warn]: Failed to resolve component: Users
If this is a native custom element, make sure to exclude it from component resolution via compilerOptions.isCustomElement. 
  at <ProjectLayout project= {projectId: 1, title: The Martian Red, description: A survival story on Mars. The protagonist finds an ancient ruin., genre: Sci-Fi, thumbnailUrl: https://images.unsplash.com/photo-1626814026160-2237a95fc5a0?w=600&auto=format} active-tab="story" scene-count=3  ... > 
  at <ProjectDetailPage onVnodeUnmounted=fn<onVnodeUnmounted> ref=Ref< Proxy(Object) > > 
  at <RouterView> 
  at <App> (at http://localhost:5173/node_modules/.vite/deps/chunk-2MKFL3BX.js?v=fcff0cfd:2194:12)
[WARNING] [Vue warn]: Failed to resolve component: Users
If this is a native custom element, make sure to exclude it from component resolution via compilerOptions.isCustomElement. 
  at <ProjectLayout project=null active-tab="story" scene-count=0  ... > 
  at <ProjectDetailPage onVnodeUnmounted=fn<onVnodeUnmounted> ref=Ref< null > > 
  at <RouterView> 
  at <App> (at http://localhost:5173/node_modules/.vite/deps/chunk-2MKFL3BX.js?v=fcff0cfd:2194:12)
[WARNING] [Vue warn]: Failed to resolve component: Users
If this is a native custom element, make sure to exclude it from component resolution via compilerOptions.isCustomElement. 
  at <ProjectLayout project= {projectId: 1, title: The Martian Red, description: A survival story on Mars. The protagonist finds an ancient ruin., genre: Sci-Fi, thumbnailUrl: https://images.unsplash.com/photo-1626814026160-2237a95fc5a0?w=600&auto=format} active-tab="story" scene-count=0  ... > 
  at <ProjectDetailPage onVnodeUnmounted=fn<onVnodeUnmounted> ref=Ref< Proxy(Object) > > 
  at <RouterView> 
  at <App> (at http://localhost:5173/node_modules/.vite/deps/chunk-2MKFL3BX.js?v=fcff0cfd:2194:12)
[WARNING] [Vue warn]: Failed to resolve component: Users
If this is a native custom element, make sure to exclude it from component resolution via compilerOptions.isCustomElement. 
  at <ProjectLayout project= {projectId: 1, title: The Martian Red, description: A survival story on Mars. The protagonist finds an ancient ruin., genre: Sci-Fi, thumbnailUrl: https://images.unsplash.com/photo-1626814026160-2237a95fc5a0?w=600&auto=format} active-tab="story" scene-count=3  ... > 
  at <ProjectDetailPage onVnodeUnmounted=fn<onVnodeUnmounted> ref=Ref< Proxy(Object) > > 
  at <RouterView> 
  at <App> (at http://localhost:5173/node_modules/.vite/deps/chunk-2MKFL3BX.js?v=fcff0cfd:2194:12)
[WARNING] [Vue warn]: Failed to resolve component: Users
If this is a native custom element, make sure to exclude it from component resolution via compilerOptions.isCustomElement. 
  at <ProjectLayout project= {projectId: 1, title: The Martian Red, description: A survival story on Mars. The protagonist finds an ancient ruin., genre: Sci-Fi, thumbnailUrl: https://images.unsplash.com/photo-1626814026160-2237a95fc5a0?w=600&auto=format} active-tab="story" scene-count=3  ... > 
  at <ProjectDetailPage onVnodeUnmounted=fn<onVnodeUnmounted> ref=Ref< Proxy(Object) > > 
  at <RouterView> 
  at <App> (at http://localhost:5173/node_modules/.vite/deps/chunk-2MKFL3BX.js?v=fcff0cfd:2194:12)
[WARNING] [Vue warn]: Failed to resolve component: Users
If this is a native custom element, make sure to exclude it from component resolution via compilerOptions.isCustomElement. 
  at <ProjectLayout project= {projectId: 1, title: The Martian Red, description: A survival story on Mars. The protagonist finds an ancient ruin., genre: Sci-Fi, thumbnailUrl: https://images.unsplash.com/photo-1626814026160-2237a95fc5a0?w=600&auto=format} active-tab="settings" scene-count=3  ... > 
  at <ProjectDetailPage onVnodeUnmounted=fn<onVnodeUnmounted> ref=Ref< Proxy(Object) > > 
  at <RouterView> 
  at <App> (at http://localhost:5173/node_modules/.vite/deps/chunk-2MKFL3BX.js?v=fcff0cfd:2194:12)
- **Test Visualization and Result:** https://www.testsprite.com/dashboard/mcp/tests/b8d10419-9992-4aee-9c91-f1d098d8c71b/c68f476b-c818-479e-910a-2fdd1719f05c
- **Status:** ❌ Failed
- **Analysis / Findings:** {{TODO:AI_ANALYSIS}}.
---

#### Test TC011 Real-time collaboration room join establishes socket connection
- **Test Code:** [TC011_Real_time_collaboration_room_join_establishes_socket_connection.py](./TC011_Real_time_collaboration_room_join_establishes_socket_connection.py)
- **Test Error:** Tested the scene editor collaboration feature by clicking the '협업 시작' button. The collabStore.joinRoom method did not initiate any socket connection or join the collaboration room as expected. No console logs or network activity were detected. Reporting this issue and stopping further testing.
Browser Console Logs:
[WARNING] [Vue warn]: Failed to resolve component: Users
If this is a native custom element, make sure to exclude it from component resolution via compilerOptions.isCustomElement. 
  at <ProjectLayout project=null active-tab="story" scene-count=0  ... > 
  at <ProjectDetailPage onVnodeUnmounted=fn<onVnodeUnmounted> ref=Ref< null > > 
  at <RouterView> 
  at <App> (at http://localhost:5173/node_modules/.vite/deps/chunk-2MKFL3BX.js?v=fcff0cfd:2194:12)
[WARNING] [Vue warn]: Failed to resolve component: Users
If this is a native custom element, make sure to exclude it from component resolution via compilerOptions.isCustomElement. 
  at <ProjectLayout project= {projectId: 1, title: The Martian Red, description: A survival story on Mars. The protagonist finds an ancient ruin., genre: Sci-Fi, thumbnailUrl: https://images.unsplash.com/photo-1626814026160-2237a95fc5a0?w=600&auto=format} active-tab="story" scene-count=0  ... > 
  at <ProjectDetailPage onVnodeUnmounted=fn<onVnodeUnmounted> ref=Ref< Proxy(Object) > > 
  at <RouterView> 
  at <App> (at http://localhost:5173/node_modules/.vite/deps/chunk-2MKFL3BX.js?v=fcff0cfd:2194:12)
[WARNING] [Vue warn]: Failed to resolve component: Users
If this is a native custom element, make sure to exclude it from component resolution via compilerOptions.isCustomElement. 
  at <ProjectLayout project= {projectId: 1, title: The Martian Red, description: A survival story on Mars. The protagonist finds an ancient ruin., genre: Sci-Fi, thumbnailUrl: https://images.unsplash.com/photo-1626814026160-2237a95fc5a0?w=600&auto=format} active-tab="story" scene-count=3  ... > 
  at <ProjectDetailPage onVnodeUnmounted=fn<onVnodeUnmounted> ref=Ref< Proxy(Object) > > 
  at <RouterView> 
  at <App> (at http://localhost:5173/node_modules/.vite/deps/chunk-2MKFL3BX.js?v=fcff0cfd:2194:12)
[WARNING] [Vue warn]: Failed to resolve component: Users
If this is a native custom element, make sure to exclude it from component resolution via compilerOptions.isCustomElement. 
  at <ProjectLayout project= {projectId: 1, title: The Martian Red, description: A survival story on Mars. The protagonist finds an ancient ruin., genre: Sci-Fi, thumbnailUrl: https://images.unsplash.com/photo-1626814026160-2237a95fc5a0?w=600&auto=format} active-tab="story" scene-count=3  ... > 
  at <ProjectDetailPage onVnodeUnmounted=fn<onVnodeUnmounted> ref=Ref< Proxy(Object) > > 
  at <RouterView> 
  at <App> (at http://localhost:5173/node_modules/.vite/deps/chunk-2MKFL3BX.js?v=fcff0cfd:2194:12)
[WARNING] [Vue warn]: Failed to resolve component: Users
If this is a native custom element, make sure to exclude it from component resolution via compilerOptions.isCustomElement. 
  at <ProjectLayout project=null active-tab="story" scene-count=0  ... > 
  at <ProjectDetailPage onVnodeUnmounted=fn<onVnodeUnmounted> ref=Ref< undefined > > 
  at <RouterView> 
  at <App> (at http://localhost:5173/node_modules/.vite/deps/chunk-2MKFL3BX.js?v=fcff0cfd:2194:12)
[WARNING] [Vue warn]: Failed to resolve component: Users
If this is a native custom element, make sure to exclude it from component resolution via compilerOptions.isCustomElement. 
  at <ProjectLayout project= {projectId: 1, title: The Martian Red, description: A survival story on Mars. The protagonist finds an ancient ruin., genre: Sci-Fi, thumbnailUrl: https://images.unsplash.com/photo-1626814026160-2237a95fc5a0?w=600&auto=format} active-tab="story" scene-count=0  ... > 
  at <ProjectDetailPage onVnodeUnmounted=fn<onVnodeUnmounted> ref=Ref< Proxy(Object) > > 
  at <RouterView> 
  at <App> (at http://localhost:5173/node_modules/.vite/deps/chunk-2MKFL3BX.js?v=fcff0cfd:2194:12)
[WARNING] [Vue warn]: Failed to resolve component: Users
If this is a native custom element, make sure to exclude it from component resolution via compilerOptions.isCustomElement. 
  at <ProjectLayout project= {projectId: 1, title: The Martian Red, description: A survival story on Mars. The protagonist finds an ancient ruin., genre: Sci-Fi, thumbnailUrl: https://images.unsplash.com/photo-1626814026160-2237a95fc5a0?w=600&auto=format} active-tab="story" scene-count=3  ... > 
  at <ProjectDetailPage onVnodeUnmounted=fn<onVnodeUnmounted> ref=Ref< Proxy(Object) > > 
  at <RouterView> 
  at <App> (at http://localhost:5173/node_modules/.vite/deps/chunk-2MKFL3BX.js?v=fcff0cfd:2194:12)
[WARNING] [Vue warn]: Failed to resolve component: Users
If this is a native custom element, make sure to exclude it from component resolution via compilerOptions.isCustomElement. 
  at <ProjectLayout project= {projectId: 1, title: The Martian Red, description: A survival story on Mars. The protagonist finds an ancient ruin., genre: Sci-Fi, thumbnailUrl: https://images.unsplash.com/photo-1626814026160-2237a95fc5a0?w=600&auto=format} active-tab="story" scene-count=3  ... > 
  at <ProjectDetailPage onVnodeUnmounted=fn<onVnodeUnmounted> ref=Ref< Proxy(Object) > > 
  at <RouterView> 
  at <App> (at http://localhost:5173/node_modules/.vite/deps/chunk-2MKFL3BX.js?v=fcff0cfd:2194:12)
- **Test Visualization and Result:** https://www.testsprite.com/dashboard/mcp/tests/b8d10419-9992-4aee-9c91-f1d098d8c71b/7529e031-c80b-4f90-a0fd-440032a864a6
- **Status:** ❌ Failed
- **Analysis / Findings:** {{TODO:AI_ANALYSIS}}.
---

#### Test TC012 Real-time collaboration room leave disconnects socket cleanly
- **Test Code:** [TC012_Real_time_collaboration_room_leave_disconnects_socket_cleanly.py](./TC012_Real_time_collaboration_room_leave_disconnects_socket_cleanly.py)
- **Test Error:** Test stopped. The collaboration start button does not initiate the socket connection as expected, so the socket disconnection on leaving the scene editor cannot be verified. Please fix this issue to enable proper testing.
Browser Console Logs:
[WARNING] [Vue warn]: Failed to resolve component: Users
If this is a native custom element, make sure to exclude it from component resolution via compilerOptions.isCustomElement. 
  at <ProjectLayout project=null active-tab="story" scene-count=0  ... > 
  at <ProjectDetailPage onVnodeUnmounted=fn<onVnodeUnmounted> ref=Ref< null > > 
  at <RouterView> 
  at <App> (at http://localhost:5173/node_modules/.vite/deps/chunk-2MKFL3BX.js?v=fcff0cfd:2194:12)
[WARNING] [Vue warn]: Failed to resolve component: Users
If this is a native custom element, make sure to exclude it from component resolution via compilerOptions.isCustomElement. 
  at <ProjectLayout project= {projectId: 1, title: The Martian Red, description: A survival story on Mars. The protagonist finds an ancient ruin., genre: Sci-Fi, thumbnailUrl: https://images.unsplash.com/photo-1626814026160-2237a95fc5a0?w=600&auto=format} active-tab="story" scene-count=0  ... > 
  at <ProjectDetailPage onVnodeUnmounted=fn<onVnodeUnmounted> ref=Ref< Proxy(Object) > > 
  at <RouterView> 
  at <App> (at http://localhost:5173/node_modules/.vite/deps/chunk-2MKFL3BX.js?v=fcff0cfd:2194:12)
[WARNING] [Vue warn]: Failed to resolve component: Users
If this is a native custom element, make sure to exclude it from component resolution via compilerOptions.isCustomElement. 
  at <ProjectLayout project= {projectId: 1, title: The Martian Red, description: A survival story on Mars. The protagonist finds an ancient ruin., genre: Sci-Fi, thumbnailUrl: https://images.unsplash.com/photo-1626814026160-2237a95fc5a0?w=600&auto=format} active-tab="story" scene-count=3  ... > 
  at <ProjectDetailPage onVnodeUnmounted=fn<onVnodeUnmounted> ref=Ref< Proxy(Object) > > 
  at <RouterView> 
  at <App> (at http://localhost:5173/node_modules/.vite/deps/chunk-2MKFL3BX.js?v=fcff0cfd:2194:12)
[WARNING] [Vue warn]: Failed to resolve component: Users
If this is a native custom element, make sure to exclude it from component resolution via compilerOptions.isCustomElement. 
  at <ProjectLayout project= {projectId: 1, title: The Martian Red, description: A survival story on Mars. The protagonist finds an ancient ruin., genre: Sci-Fi, thumbnailUrl: https://images.unsplash.com/photo-1626814026160-2237a95fc5a0?w=600&auto=format} active-tab="story" scene-count=3  ... > 
  at <ProjectDetailPage onVnodeUnmounted=fn<onVnodeUnmounted> ref=Ref< Proxy(Object) > > 
  at <RouterView> 
  at <App> (at http://localhost:5173/node_modules/.vite/deps/chunk-2MKFL3BX.js?v=fcff0cfd:2194:12)
[WARNING] [Vue warn]: Failed to resolve component: Users
If this is a native custom element, make sure to exclude it from component resolution via compilerOptions.isCustomElement. 
  at <ProjectLayout project=null active-tab="story" scene-count=0  ... > 
  at <ProjectDetailPage onVnodeUnmounted=fn<onVnodeUnmounted> ref=Ref< undefined > > 
  at <RouterView> 
  at <App> (at http://localhost:5173/node_modules/.vite/deps/chunk-2MKFL3BX.js?v=fcff0cfd:2194:12)
[WARNING] [Vue warn]: Failed to resolve component: Users
If this is a native custom element, make sure to exclude it from component resolution via compilerOptions.isCustomElement. 
  at <ProjectLayout project= {projectId: 1, title: The Martian Red, description: A survival story on Mars. The protagonist finds an ancient ruin., genre: Sci-Fi, thumbnailUrl: https://images.unsplash.com/photo-1626814026160-2237a95fc5a0?w=600&auto=format} active-tab="story" scene-count=0  ... > 
  at <ProjectDetailPage onVnodeUnmounted=fn<onVnodeUnmounted> ref=Ref< Proxy(Object) > > 
  at <RouterView> 
  at <App> (at http://localhost:5173/node_modules/.vite/deps/chunk-2MKFL3BX.js?v=fcff0cfd:2194:12)
[WARNING] [Vue warn]: Failed to resolve component: Users
If this is a native custom element, make sure to exclude it from component resolution via compilerOptions.isCustomElement. 
  at <ProjectLayout project= {projectId: 1, title: The Martian Red, description: A survival story on Mars. The protagonist finds an ancient ruin., genre: Sci-Fi, thumbnailUrl: https://images.unsplash.com/photo-1626814026160-2237a95fc5a0?w=600&auto=format} active-tab="story" scene-count=3  ... > 
  at <ProjectDetailPage onVnodeUnmounted=fn<onVnodeUnmounted> ref=Ref< Proxy(Object) > > 
  at <RouterView> 
  at <App> (at http://localhost:5173/node_modules/.vite/deps/chunk-2MKFL3BX.js?v=fcff0cfd:2194:12)
[WARNING] [Vue warn]: Failed to resolve component: Users
If this is a native custom element, make sure to exclude it from component resolution via compilerOptions.isCustomElement. 
  at <ProjectLayout project= {projectId: 1, title: The Martian Red, description: A survival story on Mars. The protagonist finds an ancient ruin., genre: Sci-Fi, thumbnailUrl: https://images.unsplash.com/photo-1626814026160-2237a95fc5a0?w=600&auto=format} active-tab="story" scene-count=3  ... > 
  at <ProjectDetailPage onVnodeUnmounted=fn<onVnodeUnmounted> ref=Ref< Proxy(Object) > > 
  at <RouterView> 
  at <App> (at http://localhost:5173/node_modules/.vite/deps/chunk-2MKFL3BX.js?v=fcff0cfd:2194:12)
- **Test Visualization and Result:** https://www.testsprite.com/dashboard/mcp/tests/b8d10419-9992-4aee-9c91-f1d098d8c71b/902f284c-f404-4af4-93f6-b8042c3d54cb
- **Status:** ❌ Failed
- **Analysis / Findings:** {{TODO:AI_ANALYSIS}}.
---

#### Test TC013 Real-time cursor and state updates reflect other user's activity instantly
- **Test Code:** [TC013_Real_time_cursor_and_state_updates_reflect_other_users_activity_instantly.py](./TC013_Real_time_cursor_and_state_updates_reflect_other_users_activity_instantly.py)
- **Test Error:** Collaboration start button is not functioning as expected, preventing further testing of real-time collaboration features. Test stopped and issue reported.
Browser Console Logs:
[WARNING] [Vue warn]: Failed to resolve component: Users
If this is a native custom element, make sure to exclude it from component resolution via compilerOptions.isCustomElement. 
  at <ProjectLayout project=null active-tab="story" scene-count=0  ... > 
  at <ProjectDetailPage onVnodeUnmounted=fn<onVnodeUnmounted> ref=Ref< null > > 
  at <RouterView> 
  at <App> (at http://localhost:5173/node_modules/.vite/deps/chunk-2MKFL3BX.js?v=fcff0cfd:2194:12)
[WARNING] [Vue warn]: Failed to resolve component: Users
If this is a native custom element, make sure to exclude it from component resolution via compilerOptions.isCustomElement. 
  at <ProjectLayout project= {projectId: 1, title: The Martian Red, description: A survival story on Mars. The protagonist finds an ancient ruin., genre: Sci-Fi, thumbnailUrl: https://images.unsplash.com/photo-1626814026160-2237a95fc5a0?w=600&auto=format} active-tab="story" scene-count=0  ... > 
  at <ProjectDetailPage onVnodeUnmounted=fn<onVnodeUnmounted> ref=Ref< Proxy(Object) > > 
  at <RouterView> 
  at <App> (at http://localhost:5173/node_modules/.vite/deps/chunk-2MKFL3BX.js?v=fcff0cfd:2194:12)
[WARNING] [Vue warn]: Failed to resolve component: Users
If this is a native custom element, make sure to exclude it from component resolution via compilerOptions.isCustomElement. 
  at <ProjectLayout project= {projectId: 1, title: The Martian Red, description: A survival story on Mars. The protagonist finds an ancient ruin., genre: Sci-Fi, thumbnailUrl: https://images.unsplash.com/photo-1626814026160-2237a95fc5a0?w=600&auto=format} active-tab="story" scene-count=3  ... > 
  at <ProjectDetailPage onVnodeUnmounted=fn<onVnodeUnmounted> ref=Ref< Proxy(Object) > > 
  at <RouterView> 
  at <App> (at http://localhost:5173/node_modules/.vite/deps/chunk-2MKFL3BX.js?v=fcff0cfd:2194:12)
[WARNING] [Vue warn]: Failed to resolve component: Users
If this is a native custom element, make sure to exclude it from component resolution via compilerOptions.isCustomElement. 
  at <ProjectLayout project= {projectId: 1, title: The Martian Red, description: A survival story on Mars. The protagonist finds an ancient ruin., genre: Sci-Fi, thumbnailUrl: https://images.unsplash.com/photo-1626814026160-2237a95fc5a0?w=600&auto=format} active-tab="story" scene-count=3  ... > 
  at <ProjectDetailPage onVnodeUnmounted=fn<onVnodeUnmounted> ref=Ref< Proxy(Object) > > 
  at <RouterView> 
  at <App> (at http://localhost:5173/node_modules/.vite/deps/chunk-2MKFL3BX.js?v=fcff0cfd:2194:12)
[WARNING] [Vue warn]: Failed to resolve component: Users
If this is a native custom element, make sure to exclude it from component resolution via compilerOptions.isCustomElement. 
  at <ProjectLayout project=null active-tab="story" scene-count=0  ... > 
  at <ProjectDetailPage onVnodeUnmounted=fn<onVnodeUnmounted> ref=Ref< undefined > > 
  at <RouterView> 
  at <App> (at http://localhost:5173/node_modules/.vite/deps/chunk-2MKFL3BX.js?v=fcff0cfd:2194:12)
[WARNING] [Vue warn]: Failed to resolve component: Users
If this is a native custom element, make sure to exclude it from component resolution via compilerOptions.isCustomElement. 
  at <ProjectLayout project= {projectId: 1, title: The Martian Red, description: A survival story on Mars. The protagonist finds an ancient ruin., genre: Sci-Fi, thumbnailUrl: https://images.unsplash.com/photo-1626814026160-2237a95fc5a0?w=600&auto=format} active-tab="story" scene-count=0  ... > 
  at <ProjectDetailPage onVnodeUnmounted=fn<onVnodeUnmounted> ref=Ref< Proxy(Object) > > 
  at <RouterView> 
  at <App> (at http://localhost:5173/node_modules/.vite/deps/chunk-2MKFL3BX.js?v=fcff0cfd:2194:12)
[WARNING] [Vue warn]: Failed to resolve component: Users
If this is a native custom element, make sure to exclude it from component resolution via compilerOptions.isCustomElement. 
  at <ProjectLayout project= {projectId: 1, title: The Martian Red, description: A survival story on Mars. The protagonist finds an ancient ruin., genre: Sci-Fi, thumbnailUrl: https://images.unsplash.com/photo-1626814026160-2237a95fc5a0?w=600&auto=format} active-tab="story" scene-count=3  ... > 
  at <ProjectDetailPage onVnodeUnmounted=fn<onVnodeUnmounted> ref=Ref< Proxy(Object) > > 
  at <RouterView> 
  at <App> (at http://localhost:5173/node_modules/.vite/deps/chunk-2MKFL3BX.js?v=fcff0cfd:2194:12)
[WARNING] [Vue warn]: Failed to resolve component: Users
If this is a native custom element, make sure to exclude it from component resolution via compilerOptions.isCustomElement. 
  at <ProjectLayout project= {projectId: 1, title: The Martian Red, description: A survival story on Mars. The protagonist finds an ancient ruin., genre: Sci-Fi, thumbnailUrl: https://images.unsplash.com/photo-1626814026160-2237a95fc5a0?w=600&auto=format} active-tab="story" scene-count=3  ... > 
  at <ProjectDetailPage onVnodeUnmounted=fn<onVnodeUnmounted> ref=Ref< Proxy(Object) > > 
  at <RouterView> 
  at <App> (at http://localhost:5173/node_modules/.vite/deps/chunk-2MKFL3BX.js?v=fcff0cfd:2194:12)
- **Test Visualization and Result:** https://www.testsprite.com/dashboard/mcp/tests/b8d10419-9992-4aee-9c91-f1d098d8c71b/1357ea4e-91c6-41a0-ba44-8771b35f3d8a
- **Status:** ❌ Failed
- **Analysis / Findings:** {{TODO:AI_ANALYSIS}}.
---

#### Test TC014 Timeline clips recompute immediately after confirmedVideos data changes
- **Test Code:** [TC014_Timeline_clips_recompute_immediately_after_confirmedVideos_data_changes.py](./TC014_Timeline_clips_recompute_immediately_after_confirmedVideos_data_changes.py)
- **Test Error:** The timeline view is stuck loading and does not display any clips or UI elements. This prevents testing the update of confirmedVideos data and verifying the recomputation of timeline clips. Reporting this issue and stopping further testing as the task cannot be completed in the current state.
Browser Console Logs:
[WARNING] [Vue warn]: Failed to resolve component: Users
If this is a native custom element, make sure to exclude it from component resolution via compilerOptions.isCustomElement. 
  at <ProjectLayout project=null active-tab="story" scene-count=0  ... > 
  at <ProjectDetailPage onVnodeUnmounted=fn<onVnodeUnmounted> ref=Ref< null > > 
  at <RouterView> 
  at <App> (at http://localhost:5173/node_modules/.vite/deps/chunk-2MKFL3BX.js?v=fcff0cfd:2194:12)
[WARNING] [Vue warn]: Failed to resolve component: Users
If this is a native custom element, make sure to exclude it from component resolution via compilerOptions.isCustomElement. 
  at <ProjectLayout project= {projectId: 1, title: The Martian Red, description: A survival story on Mars. The protagonist finds an ancient ruin., genre: Sci-Fi, thumbnailUrl: https://images.unsplash.com/photo-1626814026160-2237a95fc5a0?w=600&auto=format} active-tab="story" scene-count=0  ... > 
  at <ProjectDetailPage onVnodeUnmounted=fn<onVnodeUnmounted> ref=Ref< Proxy(Object) > > 
  at <RouterView> 
  at <App> (at http://localhost:5173/node_modules/.vite/deps/chunk-2MKFL3BX.js?v=fcff0cfd:2194:12)
[WARNING] [Vue warn]: Failed to resolve component: Users
If this is a native custom element, make sure to exclude it from component resolution via compilerOptions.isCustomElement. 
  at <ProjectLayout project= {projectId: 1, title: The Martian Red, description: A survival story on Mars. The protagonist finds an ancient ruin., genre: Sci-Fi, thumbnailUrl: https://images.unsplash.com/photo-1626814026160-2237a95fc5a0?w=600&auto=format} active-tab="story" scene-count=3  ... > 
  at <ProjectDetailPage onVnodeUnmounted=fn<onVnodeUnmounted> ref=Ref< Proxy(Object) > > 
  at <RouterView> 
  at <App> (at http://localhost:5173/node_modules/.vite/deps/chunk-2MKFL3BX.js?v=fcff0cfd:2194:12)
[WARNING] [Vue warn]: Failed to resolve component: Users
If this is a native custom element, make sure to exclude it from component resolution via compilerOptions.isCustomElement. 
  at <ProjectLayout project= {projectId: 1, title: The Martian Red, description: A survival story on Mars. The protagonist finds an ancient ruin., genre: Sci-Fi, thumbnailUrl: https://images.unsplash.com/photo-1626814026160-2237a95fc5a0?w=600&auto=format} active-tab="story" scene-count=3  ... > 
  at <ProjectDetailPage onVnodeUnmounted=fn<onVnodeUnmounted> ref=Ref< Proxy(Object) > > 
  at <RouterView> 
  at <App> (at http://localhost:5173/node_modules/.vite/deps/chunk-2MKFL3BX.js?v=fcff0cfd:2194:12)
[WARNING] [Vue warn]: Failed to resolve component: Users
If this is a native custom element, make sure to exclude it from component resolution via compilerOptions.isCustomElement. 
  at <ProjectLayout project=null active-tab="story" scene-count=0  ... > 
  at <ProjectDetailPage onVnodeUnmounted=fn<onVnodeUnmounted> ref=Ref< undefined > > 
  at <RouterView> 
  at <App> (at http://localhost:5173/node_modules/.vite/deps/chunk-2MKFL3BX.js?v=fcff0cfd:2194:12)
[WARNING] [Vue warn]: Failed to resolve component: Users
If this is a native custom element, make sure to exclude it from component resolution via compilerOptions.isCustomElement. 
  at <ProjectLayout project= {projectId: 1, title: The Martian Red, description: A survival story on Mars. The protagonist finds an ancient ruin., genre: Sci-Fi, thumbnailUrl: https://images.unsplash.com/photo-1626814026160-2237a95fc5a0?w=600&auto=format} active-tab="story" scene-count=0  ... > 
  at <ProjectDetailPage onVnodeUnmounted=fn<onVnodeUnmounted> ref=Ref< Proxy(Object) > > 
  at <RouterView> 
  at <App> (at http://localhost:5173/node_modules/.vite/deps/chunk-2MKFL3BX.js?v=fcff0cfd:2194:12)
[WARNING] [Vue warn]: Failed to resolve component: Users
If this is a native custom element, make sure to exclude it from component resolution via compilerOptions.isCustomElement. 
  at <ProjectLayout project= {projectId: 1, title: The Martian Red, description: A survival story on Mars. The protagonist finds an ancient ruin., genre: Sci-Fi, thumbnailUrl: https://images.unsplash.com/photo-1626814026160-2237a95fc5a0?w=600&auto=format} active-tab="story" scene-count=3  ... > 
  at <ProjectDetailPage onVnodeUnmounted=fn<onVnodeUnmounted> ref=Ref< Proxy(Object) > > 
  at <RouterView> 
  at <App> (at http://localhost:5173/node_modules/.vite/deps/chunk-2MKFL3BX.js?v=fcff0cfd:2194:12)
[WARNING] [Vue warn]: Failed to resolve component: Users
If this is a native custom element, make sure to exclude it from component resolution via compilerOptions.isCustomElement. 
  at <ProjectLayout project= {projectId: 1, title: The Martian Red, description: A survival story on Mars. The protagonist finds an ancient ruin., genre: Sci-Fi, thumbnailUrl: https://images.unsplash.com/photo-1626814026160-2237a95fc5a0?w=600&auto=format} active-tab="story" scene-count=3  ... > 
  at <ProjectDetailPage onVnodeUnmounted=fn<onVnodeUnmounted> ref=Ref< Proxy(Object) > > 
  at <RouterView> 
  at <App> (at http://localhost:5173/node_modules/.vite/deps/chunk-2MKFL3BX.js?v=fcff0cfd:2194:12)
[WARNING] [Vue warn]: Failed to resolve component: Users
If this is a native custom element, make sure to exclude it from component resolution via compilerOptions.isCustomElement. 
  at <ProjectLayout project= {projectId: 1, title: The Martian Red, description: A survival story on Mars. The protagonist finds an ancient ruin., genre: Sci-Fi, thumbnailUrl: https://images.unsplash.com/photo-1626814026160-2237a95fc5a0?w=600&auto=format} active-tab="story" scene-count=3  ... > 
  at <ProjectDetailPage onVnodeUnmounted=fn<onVnodeUnmounted> ref=Ref< null > > 
  at <RouterView> 
  at <App> (at http://localhost:5173/node_modules/.vite/deps/chunk-2MKFL3BX.js?v=fcff0cfd:2194:12)
[WARNING] [Vue warn]: Failed to resolve component: Users
If this is a native custom element, make sure to exclude it from component resolution via compilerOptions.isCustomElement. 
  at <ProjectLayout project= {projectId: 1, title: The Martian Red, description: A survival story on Mars. The protagonist finds an ancient ruin., genre: Sci-Fi, thumbnailUrl: https://images.unsplash.com/photo-1626814026160-2237a95fc5a0?w=600&auto=format} active-tab="story" scene-count=3  ... > 
  at <ProjectDetailPage onVnodeUnmounted=fn<onVnodeUnmounted> ref=Ref< Proxy(Object) > > 
  at <RouterView> 
  at <App> (at http://localhost:5173/node_modules/.vite/deps/chunk-2MKFL3BX.js?v=fcff0cfd:2194:12)
[WARNING] [Vue warn]: Failed to resolve component: Users
If this is a native custom element, make sure to exclude it from component resolution via compilerOptions.isCustomElement. 
  at <ProjectLayout project= {projectId: 1, title: The Martian Red, description: A survival story on Mars. The protagonist finds an ancient ruin., genre: Sci-Fi, thumbnailUrl: https://images.unsplash.com/photo-1626814026160-2237a95fc5a0?w=600&auto=format} active-tab="story" scene-count=3  ... > 
  at <ProjectDetailPage onVnodeUnmounted=fn<onVnodeUnmounted> ref=Ref< Proxy(Object) > > 
  at <RouterView> 
  at <App> (at http://localhost:5173/node_modules/.vite/deps/chunk-2MKFL3BX.js?v=fcff0cfd:2194:12)
[WARNING] [Vue warn]: Failed to resolve component: Users
If this is a native custom element, make sure to exclude it from component resolution via compilerOptions.isCustomElement. 
  at <ProjectLayout project= {projectId: 1, title: The Martian Red, description: A survival story on Mars. The protagonist finds an ancient ruin., genre: Sci-Fi, thumbnailUrl: https://images.unsplash.com/photo-1626814026160-2237a95fc5a0?w=600&auto=format} active-tab="story" scene-count=3  ... > 
  at <ProjectDetailPage onVnodeUnmounted=fn<onVnodeUnmounted> ref=Ref< Proxy(Object) > > 
  at <RouterView> 
  at <App> (at http://localhost:5173/node_modules/.vite/deps/chunk-2MKFL3BX.js?v=fcff0cfd:2194:12)
[WARNING] [Vue warn]: Failed to resolve component: Users
If this is a native custom element, make sure to exclude it from component resolution via compilerOptions.isCustomElement. 
  at <ProjectLayout project=null active-tab="story" scene-count=0  ... > 
  at <ProjectDetailPage onVnodeUnmounted=fn<onVnodeUnmounted> ref=Ref< undefined > > 
  at <RouterView> 
  at <App> (at http://localhost:5173/node_modules/.vite/deps/chunk-2MKFL3BX.js?v=fcff0cfd:2194:12)
[WARNING] [Vue warn]: Failed to resolve component: Users
If this is a native custom element, make sure to exclude it from component resolution via compilerOptions.isCustomElement. 
  at <ProjectLayout project= {projectId: 1, title: The Martian Red, description: A survival story on Mars. The protagonist finds an ancient ruin., genre: Sci-Fi, thumbnailUrl: https://images.unsplash.com/photo-1626814026160-2237a95fc5a0?w=600&auto=format} active-tab="story" scene-count=0  ... > 
  at <ProjectDetailPage onVnodeUnmounted=fn<onVnodeUnmounted> ref=Ref< Proxy(Object) > > 
  at <RouterView> 
  at <App> (at http://localhost:5173/node_modules/.vite/deps/chunk-2MKFL3BX.js?v=fcff0cfd:2194:12)
[WARNING] [Vue warn]: Failed to resolve component: Users
If this is a native custom element, make sure to exclude it from component resolution via compilerOptions.isCustomElement. 
  at <ProjectLayout project= {projectId: 1, title: The Martian Red, description: A survival story on Mars. The protagonist finds an ancient ruin., genre: Sci-Fi, thumbnailUrl: https://images.unsplash.com/photo-1626814026160-2237a95fc5a0?w=600&auto=format} active-tab="story" scene-count=3  ... > 
  at <ProjectDetailPage onVnodeUnmounted=fn<onVnodeUnmounted> ref=Ref< Proxy(Object) > > 
  at <RouterView> 
  at <App> (at http://localhost:5173/node_modules/.vite/deps/chunk-2MKFL3BX.js?v=fcff0cfd:2194:12)
[WARNING] [Vue warn]: Failed to resolve component: Users
If this is a native custom element, make sure to exclude it from component resolution via compilerOptions.isCustomElement. 
  at <ProjectLayout project= {projectId: 1, title: The Martian Red, description: A survival story on Mars. The protagonist finds an ancient ruin., genre: Sci-Fi, thumbnailUrl: https://images.unsplash.com/photo-1626814026160-2237a95fc5a0?w=600&auto=format} active-tab="story" scene-count=3  ... > 
  at <ProjectDetailPage onVnodeUnmounted=fn<onVnodeUnmounted> ref=Ref< Proxy(Object) > > 
  at <RouterView> 
  at <App> (at http://localhost:5173/node_modules/.vite/deps/chunk-2MKFL3BX.js?v=fcff0cfd:2194:12)
[WARNING] [Vue warn]: Failed to resolve component: Card
If this is a native custom element, make sure to exclude it from component resolution via compilerOptions.isCustomElement. 
  at <TimelinePage onVnodeUnmounted=fn<onVnodeUnmounted> ref=Ref< null > > 
  at <RouterView> 
  at <App> (at http://localhost:5173/node_modules/.vite/deps/chunk-2MKFL3BX.js?v=fcff0cfd:2194:12)
[WARNING] [Vue warn]: Failed to resolve component: Card
If this is a native custom element, make sure to exclude it from component resolution via compilerOptions.isCustomElement. 
  at <TimelinePage onVnodeUnmounted=fn<onVnodeUnmounted> ref=Ref< Proxy(Object) > > 
  at <RouterView> 
  at <App> (at http://localhost:5173/node_modules/.vite/deps/chunk-2MKFL3BX.js?v=fcff0cfd:2194:12)
[WARNING] [Vue warn]: Failed to resolve component: Card
If this is a native custom element, make sure to exclude it from component resolution via compilerOptions.isCustomElement. 
  at <TimelinePage onVnodeUnmounted=fn<onVnodeUnmounted> ref=Ref< Proxy(Object) > > 
  at <RouterView> 
  at <App> (at http://localhost:5173/node_modules/.vite/deps/chunk-2MKFL3BX.js?v=fcff0cfd:2194:12)
[WARNING] [Vue warn]: Failed to resolve component: Card
If this is a native custom element, make sure to exclude it from component resolution via compilerOptions.isCustomElement. 
  at <TimelinePage onVnodeUnmounted=fn<onVnodeUnmounted> ref=Ref< undefined > > 
  at <RouterView> 
  at <App> (at http://localhost:5173/node_modules/.vite/deps/chunk-2MKFL3BX.js?v=fcff0cfd:2194:12)
- **Test Visualization and Result:** https://www.testsprite.com/dashboard/mcp/tests/b8d10419-9992-4aee-9c91-f1d098d8c71b/daf789a2-8c20-4cef-9548-adcd94b3482d
- **Status:** ❌ Failed
- **Analysis / Findings:** {{TODO:AI_ANALYSIS}}.
---

#### Test TC015 Timeline totalDuration sums clip durations accurately
- **Test Code:** [TC015_Timeline_totalDuration_sums_clip_durations_accurately.py](./TC015_Timeline_totalDuration_sums_clip_durations_accurately.py)
- **Test Visualization and Result:** https://www.testsprite.com/dashboard/mcp/tests/b8d10419-9992-4aee-9c91-f1d098d8c71b/3c589a2f-9c0f-42f0-aa2c-f3fa7ba9824c
- **Status:** ✅ Passed
- **Analysis / Findings:** {{TODO:AI_ANALYSIS}}.
---

#### Test TC016 UI elements like auto-layout button maintain position after drag-and-drop
- **Test Code:** [TC016_UI_elements_like_auto_layout_button_maintain_position_after_drag_and_drop.py](./TC016_UI_elements_like_auto_layout_button_maintain_position_after_drag_and_drop.py)
- **Test Error:** The scene editor page is empty and does not display the expected UI elements such as scene nodes or the auto-layout button. Therefore, I could not perform drag-and-drop operations or verify the UI element stability as requested. Please investigate this issue.
Browser Console Logs:
[WARNING] [Vue warn]: Failed to resolve component: Users
If this is a native custom element, make sure to exclude it from component resolution via compilerOptions.isCustomElement. 
  at <ProjectLayout project=null active-tab="story" scene-count=0  ... > 
  at <ProjectDetailPage onVnodeUnmounted=fn<onVnodeUnmounted> ref=Ref< null > > 
  at <RouterView> 
  at <App> (at http://localhost:5173/node_modules/.vite/deps/chunk-2MKFL3BX.js?v=fcff0cfd:2194:12)
[WARNING] [Vue warn]: Failed to resolve component: Users
If this is a native custom element, make sure to exclude it from component resolution via compilerOptions.isCustomElement. 
  at <ProjectLayout project= {projectId: 1, title: The Martian Red, description: A survival story on Mars. The protagonist finds an ancient ruin., genre: Sci-Fi, thumbnailUrl: https://images.unsplash.com/photo-1626814026160-2237a95fc5a0?w=600&auto=format} active-tab="story" scene-count=0  ... > 
  at <ProjectDetailPage onVnodeUnmounted=fn<onVnodeUnmounted> ref=Ref< Proxy(Object) > > 
  at <RouterView> 
  at <App> (at http://localhost:5173/node_modules/.vite/deps/chunk-2MKFL3BX.js?v=fcff0cfd:2194:12)
[WARNING] [Vue warn]: Failed to resolve component: Users
If this is a native custom element, make sure to exclude it from component resolution via compilerOptions.isCustomElement. 
  at <ProjectLayout project= {projectId: 1, title: The Martian Red, description: A survival story on Mars. The protagonist finds an ancient ruin., genre: Sci-Fi, thumbnailUrl: https://images.unsplash.com/photo-1626814026160-2237a95fc5a0?w=600&auto=format} active-tab="story" scene-count=3  ... > 
  at <ProjectDetailPage onVnodeUnmounted=fn<onVnodeUnmounted> ref=Ref< Proxy(Object) > > 
  at <RouterView> 
  at <App> (at http://localhost:5173/node_modules/.vite/deps/chunk-2MKFL3BX.js?v=fcff0cfd:2194:12)
[WARNING] [Vue warn]: Failed to resolve component: Users
If this is a native custom element, make sure to exclude it from component resolution via compilerOptions.isCustomElement. 
  at <ProjectLayout project= {projectId: 1, title: The Martian Red, description: A survival story on Mars. The protagonist finds an ancient ruin., genre: Sci-Fi, thumbnailUrl: https://images.unsplash.com/photo-1626814026160-2237a95fc5a0?w=600&auto=format} active-tab="story" scene-count=3  ... > 
  at <ProjectDetailPage onVnodeUnmounted=fn<onVnodeUnmounted> ref=Ref< Proxy(Object) > > 
  at <RouterView> 
  at <App> (at http://localhost:5173/node_modules/.vite/deps/chunk-2MKFL3BX.js?v=fcff0cfd:2194:12)
[WARNING] [Vue warn]: Failed to resolve component: Users
If this is a native custom element, make sure to exclude it from component resolution via compilerOptions.isCustomElement. 
  at <ProjectLayout project=null active-tab="story" scene-count=0  ... > 
  at <ProjectDetailPage onVnodeUnmounted=fn<onVnodeUnmounted> ref=Ref< undefined > > 
  at <RouterView> 
  at <App> (at http://localhost:5173/node_modules/.vite/deps/chunk-2MKFL3BX.js?v=fcff0cfd:2194:12)
[WARNING] [Vue warn]: Failed to resolve component: Users
If this is a native custom element, make sure to exclude it from component resolution via compilerOptions.isCustomElement. 
  at <ProjectLayout project= {projectId: 1, title: The Martian Red, description: A survival story on Mars. The protagonist finds an ancient ruin., genre: Sci-Fi, thumbnailUrl: https://images.unsplash.com/photo-1626814026160-2237a95fc5a0?w=600&auto=format} active-tab="story" scene-count=0  ... > 
  at <ProjectDetailPage onVnodeUnmounted=fn<onVnodeUnmounted> ref=Ref< Proxy(Object) > > 
  at <RouterView> 
  at <App> (at http://localhost:5173/node_modules/.vite/deps/chunk-2MKFL3BX.js?v=fcff0cfd:2194:12)
[WARNING] [Vue warn]: Failed to resolve component: Users
If this is a native custom element, make sure to exclude it from component resolution via compilerOptions.isCustomElement. 
  at <ProjectLayout project= {projectId: 1, title: The Martian Red, description: A survival story on Mars. The protagonist finds an ancient ruin., genre: Sci-Fi, thumbnailUrl: https://images.unsplash.com/photo-1626814026160-2237a95fc5a0?w=600&auto=format} active-tab="story" scene-count=3  ... > 
  at <ProjectDetailPage onVnodeUnmounted=fn<onVnodeUnmounted> ref=Ref< Proxy(Object) > > 
  at <RouterView> 
  at <App> (at http://localhost:5173/node_modules/.vite/deps/chunk-2MKFL3BX.js?v=fcff0cfd:2194:12)
[WARNING] [Vue warn]: Failed to resolve component: Users
If this is a native custom element, make sure to exclude it from component resolution via compilerOptions.isCustomElement. 
  at <ProjectLayout project= {projectId: 1, title: The Martian Red, description: A survival story on Mars. The protagonist finds an ancient ruin., genre: Sci-Fi, thumbnailUrl: https://images.unsplash.com/photo-1626814026160-2237a95fc5a0?w=600&auto=format} active-tab="story" scene-count=3  ... > 
  at <ProjectDetailPage onVnodeUnmounted=fn<onVnodeUnmounted> ref=Ref< Proxy(Object) > > 
  at <RouterView> 
  at <App> (at http://localhost:5173/node_modules/.vite/deps/chunk-2MKFL3BX.js?v=fcff0cfd:2194:12)
[WARNING] [Vue warn]: Failed to resolve component: Users
If this is a native custom element, make sure to exclude it from component resolution via compilerOptions.isCustomElement. 
  at <ProjectLayout project=null active-tab="story" scene-count=0  ... > 
  at <ProjectDetailPage onVnodeUnmounted=fn<onVnodeUnmounted> ref=Ref< undefined > > 
  at <RouterView> 
  at <App> (at http://localhost:5173/node_modules/.vite/deps/chunk-2MKFL3BX.js?v=fcff0cfd:2194:12)
[WARNING] [Vue warn]: Failed to resolve component: Users
If this is a native custom element, make sure to exclude it from component resolution via compilerOptions.isCustomElement. 
  at <ProjectLayout project= {projectId: 1, title: The Martian Red, description: A survival story on Mars. The protagonist finds an ancient ruin., genre: Sci-Fi, thumbnailUrl: https://images.unsplash.com/photo-1626814026160-2237a95fc5a0?w=600&auto=format} active-tab="story" scene-count=0  ... > 
  at <ProjectDetailPage onVnodeUnmounted=fn<onVnodeUnmounted> ref=Ref< Proxy(Object) > > 
  at <RouterView> 
  at <App> (at http://localhost:5173/node_modules/.vite/deps/chunk-2MKFL3BX.js?v=fcff0cfd:2194:12)
[WARNING] [Vue warn]: Failed to resolve component: Users
If this is a native custom element, make sure to exclude it from component resolution via compilerOptions.isCustomElement. 
  at <ProjectLayout project= {projectId: 1, title: The Martian Red, description: A survival story on Mars. The protagonist finds an ancient ruin., genre: Sci-Fi, thumbnailUrl: https://images.unsplash.com/photo-1626814026160-2237a95fc5a0?w=600&auto=format} active-tab="story" scene-count=3  ... > 
  at <ProjectDetailPage onVnodeUnmounted=fn<onVnodeUnmounted> ref=Ref< Proxy(Object) > > 
  at <RouterView> 
  at <App> (at http://localhost:5173/node_modules/.vite/deps/chunk-2MKFL3BX.js?v=fcff0cfd:2194:12)
[WARNING] [Vue warn]: Failed to resolve component: Users
If this is a native custom element, make sure to exclude it from component resolution via compilerOptions.isCustomElement. 
  at <ProjectLayout project= {projectId: 1, title: The Martian Red, description: A survival story on Mars. The protagonist finds an ancient ruin., genre: Sci-Fi, thumbnailUrl: https://images.unsplash.com/photo-1626814026160-2237a95fc5a0?w=600&auto=format} active-tab="story" scene-count=3  ... > 
  at <ProjectDetailPage onVnodeUnmounted=fn<onVnodeUnmounted> ref=Ref< Proxy(Object) > > 
  at <RouterView> 
  at <App> (at http://localhost:5173/node_modules/.vite/deps/chunk-2MKFL3BX.js?v=fcff0cfd:2194:12)
- **Test Visualization and Result:** https://www.testsprite.com/dashboard/mcp/tests/b8d10419-9992-4aee-9c91-f1d098d8c71b/0cb89112-1c9e-4fc7-920f-ae0ec9c13f11
- **Status:** ❌ Failed
- **Analysis / Findings:** {{TODO:AI_ANALYSIS}}.
---

#### Test TC017 Responsive layout adapts correctly on various screen sizes
- **Test Code:** [TC017_Responsive_layout_adapts_correctly_on_various_screen_sizes.py](./TC017_Responsive_layout_adapts_correctly_on_various_screen_sizes.py)
- **Test Error:** The UI layout at the default desktop screen size is well structured with no overlap, distortion, or hidden controls. Key interactive elements such as buttons, menus, and the canvas are clearly visible and well spaced. However, the task to test UI layout adaptation fluidity across multiple screen sizes (mobile, tablet, larger desktop) was not completed as resizing tests were not performed. Therefore, full responsive behavior verification is incomplete. Further testing is recommended to resize the browser window or simulate device screen sizes to ensure no overlapping panels or controls, and that the canvas and UI scale appropriately and remain fully functional across all target screen sizes.
- **Test Visualization and Result:** https://www.testsprite.com/dashboard/mcp/tests/b8d10419-9992-4aee-9c91-f1d098d8c71b/59b108ba-8d4a-4384-9292-106310463005
- **Status:** ❌ Failed
- **Analysis / Findings:** {{TODO:AI_ANALYSIS}}.
---

#### Test TC018 User profile viewing and editing saves changes successfully
- **Test Code:** [TC018_User_profile_viewing_and_editing_saves_changes_successfully.py](./TC018_User_profile_viewing_and_editing_saves_changes_successfully.py)
- **Test Error:** Testing stopped due to critical issue: User profile section is not accessible, blocking profile view and edit functionality. Cannot proceed with profile update verification.
- **Test Visualization and Result:** https://www.testsprite.com/dashboard/mcp/tests/b8d10419-9992-4aee-9c91-f1d098d8c71b/c1052749-8980-4984-a857-5f32c0a08d27
- **Status:** ❌ Failed
- **Analysis / Findings:** {{TODO:AI_ANALYSIS}}.
---

#### Test TC019 Robust asynchronous data loading with Promise.all for project and scenes
- **Test Code:** [TC019_Robust_asynchronous_data_loading_with_Promise.all_for_project_and_scenes.py](./TC019_Robust_asynchronous_data_loading_with_Promise.all_for_project_and_scenes.py)
- **Test Visualization and Result:** https://www.testsprite.com/dashboard/mcp/tests/b8d10419-9992-4aee-9c91-f1d098d8c71b/e3bbb8c3-1955-49d7-bffe-e6922a414e59
- **Status:** ✅ Passed
- **Analysis / Findings:** {{TODO:AI_ANALYSIS}}.
---


## 3️⃣ Coverage & Matching Metrics

- **42.11** of tests passed

| Requirement        | Total Tests | ✅ Passed | ❌ Failed  |
|--------------------|-------------|-----------|------------|
| ...                | ...         | ...       | ...        |
---


## 4️⃣ Key Gaps / Risks
{AI_GNERATED_KET_GAPS_AND_RISKS}
---