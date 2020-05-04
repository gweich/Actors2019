package weichhart.georg.actor;

import java.util.HashMap;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.event.Logging;
import akka.japi.pf.ReceiveBuilder;
import weichhart.georg.communication.ResourceMessages;
import weichhart.georg.communication.ResourceMessages.Resource;
import weichhart.georg.path.Dijkstra;
import weichhart.georg.path.Node;

public class TransportOptimizeActor extends AbstractActor {
	Resource start;
	Resource stop; 
	HashMap<String,Resource> resourcePositionDB;
	ActorRef sender;
	
	public TransportOptimizeActor (Resource start, Resource stop, HashMap<String,Resource> resourcePositionDB, ActorRef sender) {
		this.start = start;
		this.stop = stop; 
		this.resourcePositionDB = resourcePositionDB;
		this.sender = sender;
	}
	
	@Override 
	public void preStart() {
		Node e = Dijkstra.searchGraph(Dijkstra.TransportPaths, start.getPositionX()+":"+start.getPositionY(), stop.getPositionX()+":"+stop.getPositionY());
		
		
		ResourceMessages.ResourcePath.Builder msg = ResourceMessages.ResourcePath.newBuilder();
		while(e!=null && e!=Node.TERMINAL_NODE) {
			msg.addPath(0, resourcePositionDB.get(e.getId()));
			e = e.getSelectedSource();
		}
		
		Logging.getLogger(this).info("path found: " + msg.toString());
		
		// send msg on behalf of parent
		sender.tell(msg.build(),getContext().getParent());
		
		getContext().stop(self());
	}

	@Override
	public Receive createReceive() {
		return ReceiveBuilder.create().build();
	}
	
}
