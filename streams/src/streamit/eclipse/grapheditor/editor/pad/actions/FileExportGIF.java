/*
 * @(#)FileExportGIF.java	1.2 01.02.2003
 *
 * Copyright (C) 2003 gaudenz alder
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 *
 */
package streamit.eclipse.grapheditor.editor.pad.actions;

import java.awt.Graphics;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.awt.image.FilteredImageSource;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import Acme.JPM.Encoders.GifEncoder;

import com.eteks.filter.Web216ColorsFilter;
import streamit.eclipse.grapheditor.editor.GPGraphpad;
import streamit.eclipse.grapheditor.editor.pad.GPConverter;
import streamit.eclipse.grapheditor.editor.pad.resources.Translator;

/**
 *
 * @author sven.luzar
 * @version 1.0
 *
 */
public class FileExportGIF extends AbstractActionFile {

	/* File type to pass to ImageIO. Default is "jpg". */
	protected transient String fileType = "gif";

	/**
	 * Constructor for FileExportJPG.
	 * @param graphpad
	 */
	public FileExportGIF(GPGraphpad graphpad) {
		this(graphpad, "gif");
	}

	public FileExportGIF(GPGraphpad graphpad, String fileType) {
		super(graphpad);
		this.fileType = fileType;
	}

	/**
	 * @see java.awt.event.ActionListener#actionPerformed(ActionEvent)
	 */
	public void actionPerformed(ActionEvent e) {
		try {
			String file =
				saveDialog(
					Translator.getString("FileSaveAsLabel") + " "+fileType.toUpperCase(),
					fileType.toLowerCase(),
					fileType.toUpperCase()+" Image");
			if (getCurrentDocument().getModel().getRootCount() > 0) {
				BufferedImage img = GPConverter.toImage(getCurrentGraph());
				
				FileOutputStream fos = new FileOutputStream(file);
				fos.write(convertToGif(img));

				// Write to file
				fos.flush();
				fos.close();
			};
		} catch (IOException ex) {
			graphpad.error(ex.getMessage());
		}
	}

	/** convert Image to GIF-encoded data, reducing the number of colors if needed */
	public static byte [] convertToGif(Image oImgBuffer) throws IOException
	{
		Graphics oGrf = oImgBuffer.getGraphics();
		ByteArrayOutputStream oOut = null;
		try {
			oOut = new ByteArrayOutputStream();
			new GifEncoder(oImgBuffer,oOut).encode();
		} catch(IOException ioe) {
			// GifEncoder throws IOException when GIF contains too many colors
			// if this happens, filter image to reduce number of colors
			final FilteredImageSource filter = new FilteredImageSource(oImgBuffer.getSource(),new Web216ColorsFilter());
			oOut = new ByteArrayOutputStream();
			new GifEncoder(filter,oOut).encode (); 
		}
		return oOut.toByteArray();
	}

}
