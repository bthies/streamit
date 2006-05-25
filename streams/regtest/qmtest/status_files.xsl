<?xml version="1.0"?>
<xsl:stylesheet version="1.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:exsl="http://exslt.org/common"
                extension-element-prefixes="exsl">
<!--
  This simple xslt file is meant to parse a qmtest results.xml file into a
  set of files, one per test.  We have processing for xsl 1.0 which does not 
  support multiple output documents, but our processor allows the 
  exslt  (http://www.exslt.org) extensions which do allow multiple outputs.

  pushd rt
  xsltproc  - - novalid status_files.xsl ../results.xml
  popd

  (spaces after '-' are just for this comment, which would be an invalid
   xml comment if they were together)

  Note: this only splits the results.xml file into files named
  for each qmtest <result  id=> attribute. with a suffix that is a 
  qmtest <result outcome=> attribute.  Nothing more.
  The qmtest outcomes that we use in our regression test are PASS, FAIL,
  ERROR, UNTESTED.  This program doe not create an output file if the outcome
  is UNTESTED.

  To get a file that our RT extension can use:
  The user still needs to combine the ID.compile.OUTCOME, ID.run.OUTCOME, 
  and ID.verify.OUTCOME files if they exist and add the proper annotations.
  Furthermore the user still needs to create listing.html, RegtestItem, 
  and Summary files.

  XSLT seems a poor language for individual processing and generating
  summary data.  We might be able to do so in 2 passes over the input.

  We could actually create combined output files using the xslt:sort function
  on the result id (since 'compile', 'run', and 'verify' are in the 
  right relation as text strings).  Even this seems a bit messy.
-->


<!-- results.xml top level is 'report', we only want reports sublevel 
 -->
<xsl:template match="/">
  <xsl:apply-templates select="report"/>
</xsl:template>

<xsl:template match="report">
  <xsl:apply-templates select="results"/>
</xsl:template>

<!-- 'results' is a sequence of 'result' records 
 -->
<xsl:template match="results">
  <xsl:apply-templates select="result"/>
</xsl:template>


<!-- for a 'result' we want a separate file containing the result 
  if the test outcome is 'UNTESTED' then we want no output at all.
--> 
<xsl:template match="result">
  <xsl:if test="@outcome!='UNTESTED'">
    <xsl:variable name="outputfile" select="@id"/>
    <xsl:variable name="outcome" select="@outcome"/>
    <exsl:document method="xml" href="{$outputfile}.{$outcome}">
       <xsl:copy-of select=".">
       </xsl:copy-of>
    </exsl:document>
  </xsl:if>
</xsl:template>

</xsl:stylesheet>
