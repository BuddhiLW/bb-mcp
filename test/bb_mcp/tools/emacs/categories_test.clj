(ns bb-mcp.tools.emacs.categories-test
  "Category tool counts and expected tools presence tests."
  (:require [clojure.test :refer :all]
            [bb-mcp.tools.emacs :as emacs]))

(def expected-tool-categories
  "Expected tool names by category for parity verification"
  {:buffer #{"eval_elisp" "emacs_status" "list_buffers" "current_buffer"
             "get_buffer_content" "find_file" "mcp_notify" "switch_to_buffer"
             "save_buffer" "goto_line" "insert_text" "recent_files" "buffer_info"
             "project_root" "mcp_capabilities" "mcp_watch_buffer"
             "mcp_list_special_buffers" "mcp_list_workflows" "mcp_run_workflow"}
   :magit #{"magit_status" "magit_branches" "magit_log" "magit_diff"
            "magit_stage" "magit_commit" "magit_push" "magit_pull"
            "magit_fetch" "magit_feature_branches"}
   :memory #{"mcp_memory_query_metadata" "mcp_memory_get_full" "mcp_memory_add"
             "mcp_memory_search_semantic" "mcp_memory_set_duration"
             "mcp_memory_promote" "mcp_memory_demote" "mcp_memory_cleanup_expired"
             "mcp_memory_expiring_soon" "mcp_memory_log_access"
             "mcp_memory_feedback" "mcp_memory_check_duplicate"}
   :kanban #{"mcp_kanban_status" "mcp_kanban_list_tasks" "mcp_kanban_create_task"
             "mcp_kanban_move_task" "mcp_kanban_update_task" "mcp_kanban_roadmap"
             "mcp_kanban_my_tasks" "mcp_kanban_sync"}
   :swarm #{"swarm_status" "swarm_spawn" "swarm_dispatch" "swarm_collect"
            "swarm_list_presets" "swarm_kill" "swarm_broadcast"
            "swarm_pending_prompts" "swarm_respond_prompt"
            "jvm_cleanup" "resource_guard"}
   :projectile #{"projectile_info" "projectile_files" "projectile_search"
                 "projectile_find_file" "projectile_recent" "projectile_list_projects"}
   :org #{"org_clj_parse" "org_clj_write" "org_clj_query" "org_kanban_render"}
   :prompt #{"prompt_capture" "prompt_list" "prompt_search" "prompt_stats"}
   :cider #{"cider_status" "cider_eval_silent" "cider_eval_explicit"
            "cider_spawn_session" "cider_list_sessions" "cider_eval_session"
            "cider_kill_session" "cider_kill_all_sessions"}
   :context #{"mcp_get_context"}})

(deftest all-expected-tools-present-test
  (testing "All expected tools from each category are present"
    (let [actual-names (set (map #(get-in % [:spec :name]) emacs/tools))]
      (doseq [[category expected-names] expected-tool-categories]
        (doseq [tool-name expected-names]
          (is (contains? actual-names tool-name)
              (str "Missing " category " tool: " tool-name)))))))

(deftest category-tool-counts-test
  (testing "Tool counts by category"
    (is (= 19 (count (:buffer expected-tool-categories))) "Buffer tools")
    (is (= 10 (count (:magit expected-tool-categories))) "Magit tools")
    (is (= 12 (count (:memory expected-tool-categories))) "Memory tools")
    (is (= 8 (count (:kanban expected-tool-categories))) "Kanban tools")
    (is (= 11 (count (:swarm expected-tool-categories))) "Swarm tools")
    (is (= 6 (count (:projectile expected-tool-categories))) "Projectile tools")
    (is (= 4 (count (:org expected-tool-categories))) "Org tools")
    (is (= 4 (count (:prompt expected-tool-categories))) "Prompt tools")
    (is (= 8 (count (:cider expected-tool-categories))) "CIDER tools")
    (is (= 1 (count (:context expected-tool-categories))) "Context tools")
    ;; Total: 19+10+12+8+11+6+4+4+8+1 = 83
    (is (= 83 (reduce + (map count (vals expected-tool-categories)))))))
