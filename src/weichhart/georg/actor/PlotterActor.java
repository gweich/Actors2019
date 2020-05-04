package weichhart.georg.actor;

import java.awt.Color;

import akka.japi.pf.ReceiveBuilder;

public class PlotterActor extends BasicActor {

	java.awt.Color c;

	public PlotterActor(String name, int posX, int posY, int color) {
		super(name, posX, posY);
		c = new Color(color);
	}
	
	@Override
	public Receive createReceive() {
		return createBuilder().build();
	}

	public ReceiveBuilder createBuilder() {
		return super.createBuilder().match(String.class, s -> s.startsWith("foo"), s -> {
			sender().tell(s.toUpperCase(), self());
		});
	}

}
