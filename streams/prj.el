;;
;; prj.el: Emacs Java Development Environment project for StreamIt
;; David Maze <dmaze@cag.lcs.mit.edu>
;; $Id: prj.el,v 1.1 2003-02-12 22:39:34 dmaze Exp $
;;

(setq jde-build-use-make t
      jde-make-args "-C $STREAMIT_HOME/compiler JAVA_OPT=\"-nowarn +F +E\""
      ; jde-run-application-class "streamit.frontend.ToJava"
      jde-run-application-class "at.dms.kjc.Main"
      jde-sourcepath '("$STREAMIT_HOME/compiler"
		       "$STREAMIT_HOME/library/java"))