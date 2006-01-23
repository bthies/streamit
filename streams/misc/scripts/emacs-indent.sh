#!/bin/sh -e
#
# emacs-indent.sh: indent a file using Emacs indenting policy, using
# spaces instead of tabs
#
emacs --batch $1 --eval "(progn (setq c-basic-offset 4) (setq java-basic-offset 4) (setq-default tab-width 4) (indent-region (point-min) (point-max) nil) (untabify (point-min) (point-max)) (save-buffer))"
