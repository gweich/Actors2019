package weichhart.georg.actor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.dispatch.OnComplete;
import akka.event.Logging;
import akka.japi.pf.ReceiveBuilder;
import scala.concurrent.Future;
import scala.concurrent.duration.FiniteDuration;
import weichhart.georg.communication.PerformativeMessages.Message;
import weichhart.georg.communication.PerformativeMessages.Message.PerformativeType;
import weichhart.georg.communication.ResourceMessages.Resource;

public abstract class BasicActor extends AbstractActor {
	
	class ActorRefCommand {
		boolean once;
		ActorRef actor;
		String command;
	}
	
	public Resource.States state = Resource.States.STATE_IDLE;
	
	public static String SUBJECT_QUERY_SUBSCRIBE_STATE = "getstate";
	
	// txt consist of the name of the PerformativeMessages.Message.PerformativeType and the command 
	public static String SUBJECT_REQUEST_WHEN_STATE = "requestwhenstate";

	public static String COMMAND_STATE_CHANGE = "changeState";
	
	
	protected String ServiceName = "???";
	protected int positionX = 1;
	protected int positionY = 0;
	
	protected ArrayList<ActorRef> subscriber = new ArrayList<>(3);
	protected HashMap<String,ActorRefCommand[]> requestWhenEver = new HashMap<>(3);

	protected ActorRef df; 

	public BasicActor(String servicename, int posX, int posY) {
		positionX = posX;
		positionY = posY;
		ServiceName = servicename;
	}


	
	@Override
	public void preStart() {
		Future<ActorRef> far = getContext().actorSelection("/*/"+DirectoryFacilitator.Name).resolveOne(new FiniteDuration(3, TimeUnit.SECONDS));
		
		BasicActor ac = this;
		far.onComplete(
			new OnComplete<ActorRef>() {
				@Override
				public void onComplete(Throwable failure, ActorRef success) throws Throwable {
					if(success != null) {
						df = success;
						df.tell(Message.newBuilder().setSource(self().path().toString())
								.setPerformative(PerformativeType.REQUEST)
								.setSubject(DirectoryFacilitator.SUBJECT_REQUEST_REGISTER)
								.setTxt(ServiceName).build(), self());
					}
					Logging.getLogger(ac).info(" got df: " + df);
				}
			}, context().dispatcher());
	}
	
	@Override 
	public void postStop() {
		// we die - tell the subscribers.
		this.state = Resource.States.STATE_SHUTTING_DOWN;
		onStateChange();
		
				
		if(df != null) {
			df.tell(Message.newBuilder().setSource(self().toString())
					.setPerformative(PerformativeType.REQUEST)
					.setSource(self().path().toString())
					.setSubject(DirectoryFacilitator.SUBJECT_REQUEST_UNREGISTER)
					.setTxt(ServiceName).build(), self());
			Logging.getLogger(this).info("derigisterd with DF ");
		}
	}
	
	
	public ReceiveBuilder createBuilder() {
		return receiveBuilder()
		 .match(Message.class,
				 m->m.getPerformative().equals(PerformativeType.QUERY),
				 m->doQuery(m))
		 .match(Message.class,
				 m->m.getPerformative().equals(PerformativeType.SUBSCRIBE),
				 m->doSubscribeToStateChange(m))
		 .match(Message.class,
				 m->m.getPerformative().equals(PerformativeType.REQUEST_WHEN),
				 m->doExecuteOnStateChange(sender(), m, true))
		 .match(Message.class,
				 m->m.getPerformative().equals(PerformativeType.REQUEST_WHENEVER),
				 m->doExecuteOnStateChange(sender(), m, false))
		 .match(Message.class, m -> m.getPerformative().equals(PerformativeType.ERROR), 
				 m -> doFailure((Message) m));
	}



	protected void doExecuteOnStateChange(ActorRef sender, Message m, boolean once) {
		ActorRefCommand arc = new ActorRefCommand();
		arc.actor=sender;
		arc.command = m.getTxt().substring(m.getTxt().indexOf(' ')+1);
		arc.once = once;
		
		String stateTo = m.getTxt().substring(0, m.getTxt().indexOf(' '));
		if(!requestWhenEver.containsKey(stateTo))
			requestWhenEver.put(stateTo, new ActorRefCommand [] {arc});
		else {
			ActorRefCommand[] newArc = new ActorRefCommand[requestWhenEver.get(stateTo).length+1];
			for(int i = requestWhenEver.get(stateTo).length-1; i >=0; --i ) {
				newArc[i] = requestWhenEver.get(stateTo)[i];
			}
			newArc[newArc.length-1] = arc;
			requestWhenEver.put(stateTo, newArc);
		}
		if(stateTo.equals(state.name())) {
			executeCommand(sender, arc.command);
		}
	}



	private void doQuery(Message m) {
		if(m.getSubject().equals(SUBJECT_QUERY_SUBSCRIBE_STATE))
			tellStateChange(sender());
	}

	private void doSubscribeToStateChange(Message m) {
		subscriber.add(sender());
		tellStateChange(sender());
	}
	
	public void onStateChange() {
		for(ActorRef subs : subscriber)
			tellStateChange(subs);
		if(requestWhenEver.containsKey(state.name())) {
			ActorRefCommand[]  arca = requestWhenEver.get(state.name());
			int remove = 0;
			for(int i = arca.length-1; i>=0; --i) {
				executeCommand(arca[i].actor,arca[i].command);
				if(arca[i].once) {
					remove++;
					// move element from the back to here; overwriting the current one.
					arca[i] = arca[arca.length-remove];
				}
			}
			if(remove > 0) {
				ActorRefCommand[] arcaNew = new ActorRefCommand[arca.length-remove];
				for(int i = 0; i<arcaNew.length;++i)
					arcaNew[i] = arca[i];
				requestWhenEver.put(state.name(), arcaNew);
			}
		}
	}
	
	public void executeCommand(ActorRef requestedBy, String command) {
		requestedBy.tell(Message.newBuilder().setPerformative(PerformativeType.INFOM)
				.setSubject(SUBJECT_REQUEST_WHEN_STATE).setTxt(state+" "+command).build(),
				self());

		if(command.startsWith(COMMAND_STATE_CHANGE)) {
			String newState = command.substring(COMMAND_STATE_CHANGE.length()+1);
			if(Resource.States.getDescriptor().findValueByName(newState)!=null) {
				state = Resource.States.forNumber(Resource.States.getDescriptor().findValueByName(newState).getIndex());
				onStateChange();
			}
		}
	}

	protected void tellStateChange(ActorRef receiver) {
		receiver.tell(Resource.newBuilder()
				.setAddress(self().path().toString())
				.setService(ServiceName)
				.setState(state)
				.setPositionX(positionX)
				.setPositionY(positionY).build(), self());
		Logging.getLogger(this).info("State Changed: " + state);
	}
	
	/**
	 * Unexpected - handle something here
	 * 
	 */
	private void doFailure(Message m) {
		Logging.getLogger(this).error(self().toString() + " received: " + m.toString());
	}


}
