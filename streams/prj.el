;;
;; prj.el: Emacs Java Development Environment project for StreamIt
;; David Maze <dmaze@cag.lcs.mit.edu>
;; $Id: prj.el,v 1.4 2003-05-20 17:28:38 dmaze Exp $
;;

(setq jde-build-function '(jde-make)
      jde-ant-enable-find t
      jde-make-args "-C $STREAMIT_HOME/compiler JAVA_OPT=\"-nowarn +F +E\""
      ; jde-run-application-class "streamit.frontend.ToJava"
      jde-run-application-class "at.dms.kjc.Main"
      jde-sourcepath (mapcar (lambda (p)
			       (file-truename
				(concat (getenv "STREAMIT_HOME") "/" p)))
			     '("compiler" "library/java")))
