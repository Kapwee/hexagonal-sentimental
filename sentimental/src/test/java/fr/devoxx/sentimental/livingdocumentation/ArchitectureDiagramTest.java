package fr.devoxx.sentimental.livingdocumentation;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.FileWriter;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.junit.Test;

import com.google.common.collect.ImmutableSet;
import com.google.common.reflect.ClassPath;
import com.google.common.reflect.ClassPath.ClassInfo;

import guru.nidi.graphviz.attribute.Arrow;
import guru.nidi.graphviz.attribute.Font;
import guru.nidi.graphviz.attribute.Label;
import guru.nidi.graphviz.attribute.RankDir;
import guru.nidi.graphviz.attribute.Shape;
import guru.nidi.graphviz.attribute.Style;
import guru.nidi.graphviz.engine.Engine;
import guru.nidi.graphviz.engine.Format;
import guru.nidi.graphviz.engine.Graphviz;
import guru.nidi.graphviz.model.Graph;
import guru.nidi.graphviz.model.MutableGraph;
import guru.nidi.graphviz.model.Node;
import guru.nidi.graphviz.model.Serializer;
import static guru.nidi.graphviz.model.Factory.*;

/**
 * Living Diagram of the Hexagonal Architecture generated out of the code thanks
 * to the package naming conventions.
 */
public class ArchitectureDiagramTest {

	@Test
	@SuppressWarnings("rawtypes")
	public void generateDiagram() throws Exception {		
		final ImmutableSet<ClassInfo> allClasses = ClassPath.from(Thread.currentThread().getContextClassLoader())
				.getTopLevelClasses();
		

		Graph graph = graph()
				.directed()
				.graphAttr().with(
						RankDir.LEFT_TO_RIGHT, 
						Label.of("Hexagonal Architecture").
						locate(Label.Location.TOP),
						Font.name("Verdana"),
						Font.size(12)
					)
				.linkAttr().with(
						Font.name("Verdana"),
						Font.size(9),
						Arrow.VEE
					)
				.nodeAttr().with(
						Shape.RECTANGLE,
						Font.name("Verdana"),
						Font.size(9)
					)
		;
		
		final String prefix = "fr.devoxx.sentimental.";
		List<ClassInfo> domain = allClasses.stream().filter(filter(prefix, "domain")).collect(Collectors.toList());
		Graph hexagon = graph().cluster().graphAttr().with(Label.of("Core Domain"));
	
		HashMap<String, Node> nodes = new HashMap<>();

		// add all domain model elements first
		for(ClassInfo ci : domain) {
			final Class clazz = ci.load();
			nodes.put(clazz.getName(), node(clazz.getSimpleName()));
			hexagon = hexagon.with(nodes.get(clazz.getName()));
		}
		graph = graph.with(hexagon);

		List<ClassInfo> infra = allClasses.stream().filter(filter(prefix, "infra")).collect(Collectors.toList());
		for(ClassInfo ci : infra) {
			final Class clazz = ci.load();
			nodes.put(clazz.getName(), node(clazz.getSimpleName()));
			graph = graph.with(nodes.get(clazz.getName()));
		}
		
		// links
		List<ClassInfo> classes = new ArrayList<>();
		classes.addAll(allClasses.stream().filter(filter(prefix, "infra")).collect(Collectors.toList()));
		classes.addAll(allClasses.stream().filter(filter(prefix, "domain")).collect(Collectors.toList()));
		for(ClassInfo ci : classes) {
			final Class clazz = ci.load();
			// API
			Graph target = graph();
			boolean link = false;
			for (Field field : clazz.getDeclaredFields()) {
				final Class<?> type = field.getType();
				if (!type.isPrimitive()) {
					if(nodes.get(type.getName()) != null && nodes.get(clazz.getName()) != null) {
						target = target.with(nodes.get(type.getName()));
						link = true;
					}
				}
			}
			if(link)
				graph = graph.with(nodes.get(clazz.getName()).link(to(target)));

			// SPI
			target = graph();
			link = false;
			for (Class intf : clazz.getInterfaces()) {
				if(nodes.get(intf.getName()) != null && nodes.get(clazz.getName()) != null) {
					target = target.with(nodes.get(intf.getName()));
					link = true;
				}
			}
			if(link)
				graph = graph.with(nodes.get(clazz.getName()).link(to(target).with(Style.DASHED, Arrow.NORMAL.open())));
		}		


		// saving dot file
		FileWriter fw = new FileWriter("hexagonal-architecture.dot");
		String src = new Serializer((MutableGraph) graph).serialize();
		fw.write(src);
		fw.close();
		// render into image
		Graphviz.fromGraph(graph).engine(Engine.DOT).render(Format.PNG).toFile(new File("hexagonal-architecture.png"));
		
		File file = new File("hexagonal-architecture.png");		
		assertThat(file).exists();
	}

	private Predicate<ClassInfo> filter(final String prefix, final String layer) {
		return new Predicate<ClassInfo>() {
			public boolean test(ClassInfo ci) {
				final boolean nameConvention = ci.getPackageName().startsWith(prefix)
						&& !ci.getSimpleName().endsWith("Test") && ci.getPackageName().contains("." + layer);
				return nameConvention;
			}

		};
	}
}
