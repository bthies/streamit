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
## $Id: Makefile,v 1.17 2001-11-09 22:10:00 thies Exp $
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
		lowering/LoweringConstants \
		lowering/Structurer \
		lowering/Namer \
		lowering/Flattener \
		lowering/SIRScheduler \
		lowering/LowerInitFunctions \
		lowering/ConstantProp \
		lowering/Fusion

MESSAGES =	../KjcMessages

OPTIONS =	../KjcOptions

## ----------------------------------------------------------------------
## GOALS

default:	build

## ----------------------------------------------------------------------
## GLOBAL DEFINITIONS

include $(TOPDIR)/Make.Defs

