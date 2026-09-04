package com.aventura.model.texture;

/**
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
 * 
 * Thrown when a Texture cannot be loaded from a file -- either because the file
 * itself couldn't be read (IOException), or because it was read but isn't a
 * recognized image format (ImageIO.read() returns null in that case, with no
 * exception of its own).
 *
 * Unchecked (extends RuntimeException) so that existing callers of
 * Texture(String) constructors -- none of which currently declare or catch a
 * checked exception -- keep compiling without change.
 *
 * @author Olivier BARRY
 * @since 2026
 *
 */
public class TextureLoadException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public TextureLoadException(String message) {
		super(message);
	}

	public TextureLoadException(String message, Throwable cause) {
		super(message, cause);
	}
}