package com.aventura.demo;

import java.awt.Color;
import java.io.File;

import com.aventura.context.PerspectiveContext;
import com.aventura.context.RenderContext;
import com.aventura.engine.RenderEngine;
import com.aventura.math.transform.Translation;
import com.aventura.math.vector.Vector3;
import com.aventura.math.vector.Vector4;
import com.aventura.model.camera.Camera;
import com.aventura.model.perspective.PerspectiveType;
import com.aventura.model.light.AmbientLight;
import com.aventura.model.light.DirectionalLight;
import com.aventura.model.light.Lighting;
import com.aventura.model.world.World;
import com.aventura.model.world.shape.Cone;
import com.aventura.model.world.shape.Sphere;
import com.aventura.model.world.shape.Trellis;
import com.aventura.view.ImageView;

/**
 * The "Hello, Aventura" program of the README, unchanged except for this comment, the package and the name of the
 * output file, which can be given as argument (default: hello.png, in the working directory). It produces the first
 * image of the README's gallery (resources/doc/images/hello_aventura.png, see DocumentationImages).
 * Keep it identical to the README.
 */
public class HelloAventura {

	public static void main(String[] args) throws Exception {

		// 1. The world: a floor, a sphere and a cone
		World world = new World();
		world.setBackgroundColor(Color.BLACK);

		Trellis floor = new Trellis(8, 8, 16, 16);
		floor.setColor(new Color(120, 120, 130));
		world.addElement(floor);

		Sphere ball = new Sphere(0.8f, 32);
		ball.setColor(new Color(220, 60, 60));
		ball.setSpecularExp(8);
		ball.setSpecularColor(new Color(180, 180, 180));
		ball.setTransformation(new Translation(new Vector4(0, 0, 0.8f, 0)));
		world.addElement(ball);

		Cone cone = new Cone(1.6f, 0.6f, 32);
		cone.setColor(new Color(60, 120, 220));
		cone.setTransformation(new Translation(new Vector4(2, 1, 0, 0)));
		world.addElement(cone);

		world.build();

		// 2. Light: one directional light (casts shadows) + a bit of ambient light
		Lighting lighting = new Lighting(new DirectionalLight(new Vector3(-1, 0.6f, -0.6f), 1.0f), new AmbientLight(0.15f), true);

		// 3. Camera: eye position, point of interest, "up" vector
		Camera camera = new Camera(new Vector4(6, -7, 4, 1), new Vector4(0, 0, 0.5f, 1), Vector4.zAxis());

		// 4. Display geometry: 0.8 x 0.45 view plane at distance 1, 1000 pixels per unit => 800x450 image
		PerspectiveContext perspective = new PerspectiveContext(0.8f, 0.45f, 1, 100, PerspectiveType.FRUSTUM, 1000);
		ImageView view = new ImageView(perspective); // off-screen: no GUI needed

		// 5. Render (smooth shading + shadows) and save
		RenderEngine engine = new RenderEngine(world, lighting, camera, RenderContext.RENDER_STANDARD_INTERPOLATE_SHADOWS, perspective);
		engine.setView(view);
		engine.render();

		view.saveImage(new File(args.length > 0 ? args[0] : "hello.png"), "png");
	}
}
