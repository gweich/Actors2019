package weichhart.georg.actor;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.event.Logging;
import weichhart.georg.communication.PerformativeMessages;
import weichhart.georg.communication.PerformativeMessages.Message;
import weichhart.georg.communication.PerformativeMessages.Message.PerformativeType;
import weichhart.georg.communication.ResourceMessages;

public class OrderCheckTransport extends AbstractActor {
	ActorRef df;
	ActorRef transporter;
	
	ActorRef mychild; 
	
	int posX; 
	int posY;
	String[] plottingServices;
	
	int plottingServicesIdx = -1;
	

	ResourceMessages.ResourcePath currentPath;

	
	public OrderCheckTransport(ActorRef df, int posX, int posY, String[] plottingServices, int plottingServicesIdx) {
		this.df = df;
		this.posX = posX;
		this.posY = posY;
		this.plottingServices = plottingServices;
		this.plottingServicesIdx = plottingServicesIdx;
	}

	@Override
	public void preStart() {
		// we will receive an ActorRef with DirectoryFacilitator.SUBJECT_QUERY_ACTOR
		// we will receive a resource object with DirectoryFacilitator.SUBJECT_QUERY_RESOURCE txt == ActorRef.toString()
		df.tell(Message.newBuilder()
				.setPerformative(PerformativeType.QUERY)
				.setSource(self().path().toString())
				.setSubject(DirectoryFacilitator.SUBJECT_QUERY_ACTOR)
				.setTxt(TransportActor.Name+0)
				.build()
				,self());
	}
	
	@Override
	public Receive createReceive() {
		
		return receiveBuilder().match(ActorRef.class, ar -> {
			// We query for a transporter actor to get the path. 
			
			transporter = ar;
			
			Logging.getLogger(this).info("received transporter " + transporter + " ask for path");
			
			// ask for the way to our goal
			transporter.tell(Message.newBuilder()
					.setSubject(TransportActor.SUBJECT_QUERY_START_STOP)
					.setTxt(posX + ":" + posY + " " + plottingServices[plottingServicesIdx+1])
					.setPerformative(PerformativeType.QUERY_IF)
					.setSource(self().path().toString())
					.build()
					, self());
			// currently the indx is -1 when we start ... 

		}).match(ResourceMessages.ResourcePath.class, rp -> {
			currentPath = rp;

			mychild = getContext().actorOf(Props.create(OrderCheckPathActor.class, currentPath));

		}).match(PerformativeMessages.Message.class, m -> getSender().equals(mychild),  m -> {
			getContext().getParent().tell(m, self());
			getContext().stop(self());
		}).build();
	}

}