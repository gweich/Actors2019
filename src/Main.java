import java.awt.Color;

import akka.actor.ActorSystem;
import akka.actor.Props;
import weichhart.georg.actor.DirectoryFacilitator;
import weichhart.georg.actor.OrderActor;
import weichhart.georg.actor.PlotterActor;
import weichhart.georg.actor.TransportActor;
import weichhart.georg.dijkstra.Dijkstra;
import weichhart.georg.dijkstra.Edge;
import weichhart.georg.dijkstra.Node;

public class Main {
	
	public static void main (String[] args) {
		ActorSystem as = ActorSystem.create("gw");
		
		as.actorOf(Props.create(DirectoryFacilitator.class), DirectoryFacilitator.Name);
	
		as.actorOf(Props.create(PlotterActor.class, "plotter-gruen", 1, 0, Color.GREEN.getRGB()), "PlotterGruen");
		as.actorOf(Props.create(PlotterActor.class, "plotter-blau", 2, 0, Color.BLUE.getRGB()), "PlotterBlau");
		as.actorOf(Props.create(PlotterActor.class, "plotter-rot", 1, 2, Color.RED.getRGB()), "PlotterRot");
		as.actorOf(Props.create(PlotterActor.class, "plotter-gelb", 2, 2, Color.YELLOW.getRGB()), "PlotterGelb");
		
		as.actorOf(Props.create(TransportActor.class,1,1),"Transport0");
		as.actorOf(Props.create(TransportActor.class,2,1),"Transport1");
		
		
		// input
		Node n01 = new Node("0:1");
		// output
		Node n31 = new Node("3:1");
		// transport
		Node n11 = new Node("1:1");
		Node n21 = new Node("2:1");
		// plotter
		Node n10 = new Node("1:0");
		Node n20 = new Node("2:0");
		Node n12 = new Node("1:2");
		Node n22 = new Node("2:2");
		
		// input
		Edge e = new Edge();
		e.setWeight(1);
		e.setTo(n11);
		n01.addEdgeTo(e);
		
		// output
		e = new Edge();
		e.setWeight(1);
		e.setTo(n21);
		n31.addEdgeTo(e);

		
		// transport 1
		e = new Edge();
		e.setWeight(1);
		e.setTo(n01);
		n11.addEdgeTo(e);
		e = new Edge();
		e.setWeight(1);
		e.setTo(n10);
		n11.addEdgeTo(e);
		e = new Edge();
		e.setWeight(1);
		e.setTo(n12);
		n11.addEdgeTo(e);
		e = new Edge();
		e.setWeight(1);
		e.setTo(n21);
		n11.addEdgeTo(e);
		
		//transport 2
		e = new Edge();
		e.setWeight(1);
		e.setTo(n20);
		n21.addEdgeTo(e);
		e = new Edge();
		e.setWeight(1);
		e.setTo(n22);
		n21.addEdgeTo(e);
		e = new Edge();
		e.setWeight(1);
		e.setTo(n11);
		n21.addEdgeTo(e);
		e = new Edge();
		e.setWeight(1);
		e.setTo(n31);
		n21.addEdgeTo(e);
		
		// plotter 1
		e = new Edge();
		e.setWeight(1);
		e.setTo(n11);
		n10.addEdgeTo(e);

		// plotter 3
		e = new Edge();
		e.setWeight(1);
		e.setTo(n11);
		n12.addEdgeTo(e);

		
		//plotter 2
		e = new Edge();
		e.setWeight(1);
		e.setTo(n21);
		n20.addEdgeTo(e);

		//plotter 4
		e = new Edge();
		e.setWeight(1);
		e.setTo(n21);
		n22.addEdgeTo(e);

		
		Dijkstra.TransportPaths = n01;
		
		// starts at pos 0:1
		as.actorOf(Props.create(OrderActor.class,"order1", 0, 1, new String[] {"plotter-gelb", "plotter-rot"} ),"Order1");
	}

}
