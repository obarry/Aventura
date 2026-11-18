/*
 * ------------------------------------------------------------------------------
 * MIT License
 *
 * Copyright (c) 2016-2026 Olivier BARRY
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 * ------------------------------------------------------------------------------
 */
package com.aventura.demo;

import java.net.URL;

import com.aventura.model.texture.Texture;
import com.aventura.model.texture.TextureLoadException;

/**
 * Where the demos find their textures: in the resources of the aventura-demos module ({@code src/main/resources/texture}),
 * read from the classpath. They are found the same way whether a demo runs from Eclipse, from Maven or from a jar, and
 * whatever the current directory.
 * <pre>
 *   Texture marble = new Texture(DemoTextures.url("texture_stone_1023x852.jpg"));
 * </pre>
 * This is the way for any application to ship its textures: put them in its resources and load them with
 * {@code new Texture(MyApp.class.getResource("/folder/name.jpg"))}. Class.getResource() is given a class of the
 * application, so that the engine does not need to see the resources of the application.
 *
 * @since 3.3
 */
public final class DemoTextures {

	/** The folder of the textures, in the resources of the demos */
	public static final String FOLDER = "/texture/";

	private DemoTextures() {
	}

	/**
	 * @param fileName the name of a texture file of the demos, e.g. "texture_stone_1023x852.jpg"
	 * @return its URL, to give to new Texture(URL)
	 * @throws TextureLoadException if there is no such texture
	 */
	public static URL url(String fileName) {
		URL url = DemoTextures.class.getResource(FOLDER + fileName);
		if (url == null) {
			throw new TextureLoadException("No texture " + fileName + " in the resources of the demos (" + FOLDER + ")");
		}
		return url;
	}

	/**
	 * @param fileName the name of a texture file of the demos
	 * @return the Texture read from it
	 */
	public static Texture load(String fileName) {
		return new Texture(url(fileName));
	}
}
