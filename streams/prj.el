;;
;; prj.el: Emacs Java Development Environment project for StreamIt
;; David Maze <dmaze@cag.lcs.mit.edu>
;; $Id: prj.el,v 1.2 2003-05-15 20:37:11 dmaze Exp $
;;

(setq jde-build-function '(jde-ant-build)
      jde-ant-enable-find t
      jde-make-args "-C $STREAMIT_HOME/compiler JAVA_OPT=\"-nowarn +F +E\""
      ; jde-run-application-class "streamit.frontend.ToJava"
      jde-run-application-class "at.dms.kjc.Main"
      jde-sourcepath '("$STREAMIT_HOME/compiler"
		       "$STREAMIT_HOME/library/java"))