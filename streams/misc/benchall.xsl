<?xml version="1.0" ?>
<xsl:stylesheet version="1.0"
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

  <xsl:template match="/benchset">
    <html>
      <head>
        <title>StreamIt Benchmarks</title>
      </head>
      <body>
        <h1>StreamIt Benchmarks</h1>
        <xsl:apply-templates/>
      </body>
    </html>
  </xsl:template>

  <!-- For directories without subdirectories, don't print a header. -->
  <xsl:template match="dir[not(dir)]">
    <xsl:param name="subdir">.</xsl:param>
    <xsl:apply-templates>
      <xsl:with-param name="subdir">
        <xsl:value-of select="$subdir"/>/<xsl:value-of select="@name"/>
      </xsl:with-param>
    </xsl:apply-templates>      
  </xsl:template>

  <xsl:template match="dir">
    <xsl:param name="subdir">.</xsl:param>
    <h2><xsl:value-of select="@name"/></h2>
    <xsl:apply-templates>
      <xsl:with-param name="subdir">
        <xsl:value-of select="$subdir"/>/<xsl:value-of select="@name"/>
      </xsl:with-param>
    </xsl:apply-templates>
  </xsl:template>

  <xsl:template match="benchmark">
    <xsl:param name="subdir">.</xsl:param>
    <h3><xsl:value-of select="name"/> - <xsl:value-of select="desc"/></h3>
    <p><xsl:value-of select="description"/></p>
    <xsl:apply-templates select="reference"/>
    <xsl:apply-templates select="implementations">
      <xsl:with-param name="subdir" select="$subdir"/>
    </xsl:apply-templates>
  </xsl:template>

  <xsl:template match="reference">
    <p><b>Reference:</b> <xsl:apply-templates/></p>
  </xsl:template>

  <xsl:template match="implementations">
    <xsl:param name="subdir">.</xsl:param>
    <!-- <p><b>Implementations:</b><br/> -->
    <xsl:apply-templates>
      <xsl:with-param name="subdir" select="$subdir"/>
    </xsl:apply-templates>
    <!-- </p> -->
  </xsl:template>

  <xsl:template match="impl">
    <!-- Save the declared subdirectory, if any. -->
    <xsl:param name="subdir">.</xsl:param>
    <xsl:variable name="path">
      <xsl:value-of select="$subdir"/>
      <xsl:if test="@dir">
        <xsl:text>/</xsl:text><xsl:value-of select="@dir"/>
      </xsl:if>
      <xsl:text>/</xsl:text>
    </xsl:variable>
    <p><b><xsl:value-of select="@lang"/></b>:
    <xsl:if test="desc"><xsl:value-of select="desc"/><br/></xsl:if>
    <xsl:for-each select="file">
      <xsl:element name="a">
        <xsl:attribute name="href">
          <xsl:value-of select="concat($path,.)"/>
        </xsl:attribute>
        <xsl:value-of select="."/>
      </xsl:element>
      <xsl:text> </xsl:text>
    </xsl:for-each>
  </p>
  </xsl:template>
</xsl:stylesheet>
