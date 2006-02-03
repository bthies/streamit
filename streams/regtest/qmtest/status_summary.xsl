<?xml version="1.0"?>
<xsl:stylesheet version="1.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
<!--
  This simple xslt file is meant to parse a qmtest results.sml file into a form
  where we can easily determine what input passed what tests.

  It is going to print out the xml version line by default (unless I learn 
  how to turn it off)  It then prints the test id and result for each test
  where the result is not 'UNTESTED'

-->


<!-- results.xml top level is "report", we only want reports sublevel 
 -->
<xsl:template match="/">
  <xsl:apply-templates select="report"/>
</xsl:template>

<xsl:template match="report">
  <xsl:apply-templates select="results"/>
</xsl:template>

<!-- results is a sequence of result records 
 -->
<xsl:template match="results">
  <xsl:apply-templates select="result"/>
</xsl:template>


<!-- in a result we just want the id and outcome attributes
 separated by a space and terminated by a newline 
--> 
<xsl:template match="result">
  <xsl:if test="@outcome!='UNTESTED'">
    <xsl:apply-templates select="@id"/><xsl:text> </xsl:text>
    <xsl:apply-templates select="@outcome"/><xsl:text>
</xsl:text>
  </xsl:if>
</xsl:template>


</xsl:stylesheet>
