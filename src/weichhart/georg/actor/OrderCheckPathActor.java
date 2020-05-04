package weichhart.georg.actor;

import java.time.Duration;

import akka.actor.AbstractActor;
import akka.actor.ReceiveTimeout;
import akka.event.Logging;
import akka.japi.pf.ReceiveBuilder;
import weichhart.georg.communication.PerformativeMessages.Message;
import weichhart.georg.communication.PerformativeMessages.Message.PerformativeType;
import weichhart.georg.communication.ResourceMessages;
import weichhart.georg.communication.ResourceMessages.Resource;

public class OrderCheckPathActor extends AbstractActor {

	ResourceMessages.ResourcePath currentPath; 
	int answers = 0;
	
	public OrderCheckPathActor(ResourceMessages.ResourcePath reservePath) {
		currentPath = reservePath;
	}

	@Override
	public Receive createReceive() {

		return receiveBuilder().match(Message.class, m -> {

			// determine if all reseources have been
			Logging.getLogger(this).info("received M:" + m);
			answers++;
			
			if(answers==currentPath.getPathCount()) {
				getContext().getParent().tell(Message.newBuilder()
						.setPerformative(PerformativeType.INFOM)
						.setSubject("done")
						.setSource(self().path().toString())
						.build()
						, self());
				getContext().cancelReceiveTimeout();
				// cancel not working ??
				getContext().setReceiveTimeout(scala.concurrent.duration.Duration.Undefined());
			}
		}).match(ReceiveTimeout.class, rt -> {
			Logging.getLogger(this).info("timeout try to get path reserved");
			
			getContext().cancelReceiveTimeout();
			// cancel not working ??
			getContext().setReceiveTimeout(scala.concurrent.duration.Duration.Undefined());
			
			getContext().getParent().tell(Message.newBuilder()
					.setPerformative(PerformativeType.FAILURE)
					.setSubject("done")
					.setSource(self().path().toString())
					.build()
					, self());
			
			getContext().stop(self());
		}).build();
	}

	@Override
	public void preStart() {
		
		getContext().setReceiveTimeout(Duration.ofSeconds(4));
		
		// check if path is available by contacting actors!! + next service
		for (Resource r : currentPath.getPathList()) {

			getContext().actorSelection(r.getAddress())
					.tell(Message.newBuilder().setPerformative(PerformativeType.REQUEST_WHEN)
							.setSource(self().path().toString()).setSubject(BasicActor.SUBJECT_REQUEST_WHEN_STATE)
							.setTxt(ResourceMessages.Resource.States.STATE_IDLE.name() + " "
									+ BasicActor.COMMAND_STATE_CHANGE + " "
									+ ResourceMessages.Resource.States.STATE_RESERVED.name())
							.build(), self());

		}

		// missing next serivce actor - we have only service name
		// getContext().actorSelection(
	}
}
