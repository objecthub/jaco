(setq sz-project-compile-command "cd $PRO; make all")
(setq compile-command sz-project-compile-command)
(defun sz-project-test (&optional args)
  (interactive "sArguments:")
  (compile (concat "cd $PRO/tests; cjava " args " Test.java"))
  (setq compile-command sz-project-compile-command))
(defun sz-project-compile ()
  (interactive)
  (compile (setq compile-command sz-project-compile-command)))
(defun sz-project-check ()
  (interactive)
  (compile "cd $PRO; make check")
  (setq compile-command sz-project-compile-command))


(global-set-key "\C-cpt" 'sz-project-test)
(global-set-key "\C-cpc" 'sz-project-check)
(global-set-key "\C-cpa" 'sz-project-compile)


(desktop-read)
