##
## Copyright (C) 1990-2001 DMS Decision Management Systems Ges.m.b.H.
##
## This program is free software; you can redistribute it and/or modify
## it under the terms of the GNU General Public License as published by
## the Free Software Foundation; either version 2 of the License, or
## (at your option) any later version.
##
## This program is distributed in the hope that it will be useful,
## but WITHOUT ANY WARRANTY; without even the implied warranty of
## MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
## GNU General Public License for more details.
##
## You should have received a copy of the GNU General Public License
## along with this program; if not, write to the Free Software
## Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
##
## $Id: Makefile,v 1.25 2002-06-15 06:48:07 thies Exp $
##

TOPDIR =	../..

PACKAGE =	at/dms/kjc/sir

## ----------------------------------------------------------------------
## TOOLS

## ----------------------------------------------------------------------
## FILES

GRM1VOC =	../Kjc
GRM1DEP =
SCANNER1 =	../Kjc

# classes
JAVAFILES +=	StreamVisitor \
		SIRBuilder \
		SIRPrintStatement \
		SIRLatency \
		SIRLatencyMax \
		SIRLatencyRange \
		SIRLatencySet \
		SIRFeedbackLoop \
		SIRFilter \
		SIRContainer \
		SIRCreatePortal \
		SIRJoinType \
		SIRJoiner \
		SIRMessageStatement \
		SIROperator \
		SIRTwoStageFilter \
		SIRPeekExpression \
		SIRPipeline \
		SIRPopExpression \
		SIRPushExpression \
		SIRRegReceiverStatement \
		SIRRegSenderStatement \
		SIRSplitJoin \
		SIRSplitType \
		SIRSplitter \
		SIRInitStatement \
		SIRStream \
		AttributeStreamVisitor \
		SIRFileWriter \
		SIRFileReader \
		Finalizer \
		Memoizer \
		EmptyStreamVisitor \
		lowering/AdjustGranularity \
		lowering/LoweringConstants \
		lowering/Structurer \
		lowering/Namer \
		lowering/Flattener \
		lowering/SIRScheduler \
		lowering/LowerInitFunctions \
		lowering/ConstantProp \
		lowering/fusion/FusePipe \
		lowering/fusion/FuseAll \
		lowering/RenameAll \
		lowering/LowerWorkFunctions \
		lowering/Unroller

MESSAGES =	../KjcMessages

OPTIONS =	../KjcOptions

## ----------------------------------------------------------------------
## GOALS

default:	build

## ----------------------------------------------------------------------
## GLOBAL DEFINITIONS

include $(TOPDIR)/Make.Defs

