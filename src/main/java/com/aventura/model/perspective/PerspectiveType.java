package com.aventura.model.perspective;

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
 * Type of a Perspective, i.e. of its projection:
 * - FRUSTUM: perspective projection (vanishing point, objects shrink with distance). The depth
 *   stored in the ZBuffer is the eye-space distance (w after projection).
 * - ORTHOGRAPHIC: parallel projection (no shrinking with distance), e.g. for DirectionalLight
 *   shadow maps or technical views. The depth stored in the ZBuffer is the NDC z, in [0, 1].
 *
 * Replaces the former PerspectiveType.FRUSTUM / PERSPECTIVE_TYPE_ORTHOGRAPHIC
 * int constants.
 *
 * @author Olivier BARRY
 * @since September 2026
 */
public enum PerspectiveType {
	FRUSTUM,
	ORTHOGRAPHIC
}
