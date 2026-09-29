package com.aventura.cookbook;

import java.awt.Color;
import java.io.File;

import com.aventura.context.PerspectiveContext;
import com.aventura.context.RenderContext;
import com.aventura.demo.DemoScene;
import com.aventura.math.transform.Rotation;
import com.aventura.math.transform.Translation;
import com.aventura.math.vector.Vector3;
import com.aventura.math.vector.Vector4;
import com.aventura.model.light.AmbientLight;
import com.aventura.model.light.DirectionalLight;
import com.aventura.model.light.Lighting;
import com.aventura.model.perspective.PerspectiveType;
import com.aventura.model.world.World;
import com.aventura.model.world.shape.Trellis;
import com.aventura.view.ImageView;

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
 * Renders the cookbook's example shapes off-screen and saves the image used in
 * docs/GEOMETRY_COOKBOOK.md (a program, not a unit test: run it from the project root).
 *
 *     java -Djava.awt.headless=true -cp target/classes:target/test-classes com.aventura.cookbook.CookbookGallery [file.png]
 *
 * @author Olivier BARRY
 * @since 2026
 */
public class CookbookGallery {

	public static World createWorld() {
		World world = new World();
		world.setBackgroundColor(new Color(20, 24, 32));

		Trellis ground = new Trellis(12, 8, 24, 16);
		ground.setColor(new Color(125, 125, 130));
		ground.setTransformation(new Translation(new Vector3(0.5f, 1, 0)));
		world.addElement(ground);

		// Recipe 1: a new primitive, with per-face colors
		PyramidFrustum plinth = new PyramidFrustum(1.4f, 1.4f, 0.8f, 0.8f, 0.9f);
		plinth.setColor(new Color(200, 170, 120));
		plinth.setTopColor(new Color(230, 120, 60));
		plinth.setTransformation(new Rotation((float) Math.toRadians(20), Vector3.zAxis()));
		plinth.combineTransformation(new Translation(new Vector3(-2.2f, 0, 0.45f)));
		world.addElement(plinth);

		// Recipe 3: an existing shape enriched by inheritance (a cone frustum closed by 2 discs)
		ClosedConeFrustum bucket = new ClosedConeFrustum(3f, 1.2f, 0.7f, 24);
		bucket.setColor(new Color(70, 130, 200));
		bucket.setTopColor(new Color(230, 230, 235));
		bucket.setSpecularExp(12);
		bucket.setSpecularColor(new Color(160, 160, 160));
		// Its frame is centered on the full cone: lift it by coneHeight/2 to put its base on the ground
		bucket.setTransformation(new Translation(new Vector3(-0.2f, -0.4f, 1.5f)));
		world.addElement(bucket);

		// Recipe 4: an assembly, instantiated twice (two distinct objects), the second one rotated
		StreetLamp lamp1 = new StreetLamp(2.6f, (float) Math.toRadians(12));
		lamp1.setTransformation(new Translation(new Vector3(1.4f, 1.2f, 0)));
		world.addElement(lamp1);

		StreetLamp lamp2 = new StreetLamp(2.2f, 0);
		lamp2.setTransformation(new Rotation((float) Math.toRadians(-120), Vector3.zAxis()));
		lamp2.combineTransformation(new Translation(new Vector3(3.6f, 2.4f, 0)));
		world.addElement(lamp2);

		world.build();
		return world;
	}

	/** The gallery scene, to be rendered off-screen (render()) or shown in a window (SceneViewer) */
	public static DemoScene createScene(int pixelsPerUnit) {
		Lighting lighting = new Lighting(new DirectionalLight(new Vector3(-0.8f, 0.9f, -0.9f), 1.0f), new AmbientLight(0.2f), true);
		PerspectiveContext perspective = new PerspectiveContext(0.8f, 0.45f, 1, 100, PerspectiveType.FRUSTUM, pixelsPerUnit);
		return new DemoScene(createWorld(), lighting, new Vector4(4.5f, -8.5f, 4.2f, 1), new Vector4(0.6f, 0.6f, 1.0f, 1),
				RenderContext.RENDER_STANDARD_INTERPOLATE_SHADOWS, perspective);
	}

	public static ImageView render(int pixelsPerUnit) {
		DemoScene scene = createScene(pixelsPerUnit);
		ImageView view = new ImageView(scene.getPerspective());
		scene.createEngine(scene.createCamera(), view).render();
		return view;
	}

	public static void main(String[] args) throws Exception {
		String file = args.length > 0 ? args[0] : "resources/doc/images/cookbook_gallery.png";
		render(1000).saveImage(new File(file), "png");
		System.out.println("Saved " + file);
	}
}
