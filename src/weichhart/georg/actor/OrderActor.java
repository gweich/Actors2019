package weichhart.georg.actor;

import java.time.Duration;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.ReceiveTimeout;
import akka.event.Logging;
import akka.japi.pf.ReceiveBuilder;
import weichhart.georg.communication.PerformativeMessages;
import weichhart.georg.communication.ResourceMessages.Resource;

public class OrderActor extends BasicActor {
	
	String[] plottingServices;
	
	int plottingServicesIdx = -1;
	
	ActorRef mychild ;
	
	
	
	/** it will hold the current position but start at -1, -1 */
	public OrderActor(String name, int startingPosX, int startingPosY, String[] plotting) {
		super(name,startingPosX,startingPosY);
		plottingServices = plotting;
	}

	@Override 
	public void preStart() {
		// register with DF 
		super.preStart();
		
		// DF is not now available but we have to wait for some time
		getContext().setReceiveTimeout(Duration.ofSeconds(4));

	}

	@Override
	public Receive createReceive() {
		return createBuilder().build();
	}

	@Override
	public ReceiveBuilder createBuilder() {
		
		return super.createBuilder().match(Resource.class, m -> {
			// position or state change of an object
			doInformResource((Resource) m);
		}).match(PerformativeMessages.Message.class, m -> getSender().equals(mychild),  m -> {
			Logging.getLogger(this).info("got checked path : " + m.getPerformative());
			
			
		}).match(ReceiveTimeout.class, rt -> {
			receivedTimeout();
		});
	}

	protected void doInformResource(Resource m) {
		
		Logging.getLogger(this).info("received Res.: " + m);
		
		if(this.positionX == m.getPositionX() && this.positionY == m.getPositionY()) {
			// are we at this resource?? waiting to be ready?

			// I am here waiting for a state change of the resource
			if(m.getState() == Resource.States.STATE_RUNNING) {
				// it switched to running
				plottingServicesIdx++;
				Logging.getLogger(this).info("my Resource ("+m.getAddress()+") is now running; working on " + 
						(plottingServicesIdx>=0&&plottingServicesIdx<plottingServices.length?plottingServices[plottingServicesIdx]: "idx: " + plottingServicesIdx));
			} else if(m.getState() == Resource.States.STATE_IDLE) {
				// the machine is now ready 
			
			} else if(m.getState() == Resource.States.STATE_ERROR || m.getState() == Resource.States.STATE_DEAD || m.getState() == Resource.States.STATE_SHUTTING_DOWN) {
				// ok we need to get away from here; but is it still possible ????
				
			}
		} else {
			// I am not at that resource, I want to go there
			if(m.getState() == Resource.States.STATE_IDLE) {
				// the machine is now ready 
				
				
				
			}
		}
	}
		
	protected void receivedTimeout() {
		Logging.getLogger(this).info("timeout ; got DF: " +df);
		
		if(df!=null) {
			
			getContext().cancelReceiveTimeout();
			// cancel not working ??
			getContext().setReceiveTimeout(scala.concurrent.duration.Duration.Undefined());
			
			
			mychild = getContext().actorOf(Props.create(OrderCheckTransport.class, df, positionX, positionY, plottingServices, plottingServicesIdx));
			
		} else {
			// timeout will keep running
		}
	}


	
	
}
