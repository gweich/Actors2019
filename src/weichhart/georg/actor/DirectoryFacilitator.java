package weichhart.georg.actor;

import java.util.HashMap;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Terminated;
import akka.event.Logging;
import akka.japi.pf.ReceiveBuilder;
import weichhart.georg.communication.PerformativeMessages.Message;
import weichhart.georg.communication.PerformativeMessages.Message.PerformativeType;
import weichhart.georg.communication.ResourceMessages.Resource;

public class DirectoryFacilitator extends AbstractActor {

	public static String Name = "DirectoryFacilitator";

	public static String SUBJECT_REQUEST_REGISTER = "register";
	public static String SUBJECT_REQUEST_UNREGISTER = "UNregister";

	public static String SUBJECT_QUERY_ALL_ACTORS = "query-all-actors";
	public static String SUBJECT_QUERY_ALL_RESOURCES = "query-all-resources";
	public static String SUBJECT_QUERY_ACTOR = "query-actor";
	public static String SUBJECT_QUERY_RESOURCE = "query-resource";

	HashMap<String, ActorRef> serviceDB = new HashMap<String, ActorRef>();
	HashMap<String, Resource> resourceDB = new HashMap<String, Resource>();

	@Override
	public Receive createReceive() {
		return createBuilder().build();
	}

	public ReceiveBuilder createBuilder() {
		return receiveBuilder().match(Double.class, d -> {
			sender().tell(d.isNaN() ? 0 : d, self());
		}).match(Integer.class, i -> {
			sender().tell(i * 10, self());
		}).match(String.class, s -> s.startsWith("foo"), s -> {
			sender().tell(s.toUpperCase(), self());
		}).match(Message.class, m -> m.getPerformative().equals(PerformativeType.REQUEST), m -> {
			doRequest((Message) m);
		}).match(Message.class, m -> m.getPerformative().equals(PerformativeType.QUERY), m -> {
			doQuery((Message) m);
		}).match(Message.class, m -> m.getPerformative().equals(PerformativeType.FAILURE), m -> {
			doFailure((Message) m);
		}).match(Message.class, m -> m.getPerformative().equals(PerformativeType.ERROR), m -> {
			doFailure((Message) m);
		}).match(Message.class, m -> m.getPerformative().equals(PerformativeType.REFUSE), m -> {
			doFailure((Message) m);
		}).match(Resource.class, m -> {
			doInformResource((Resource) m);
		}).match(Terminated.class, m -> {
			watchedActorTerminated((Terminated) m);
		});
	}

	/**
	 * (de-)register actor in DF; in turn the DF subscribes to state changes of sender
	 * 
	 */
	private void doRequest(Message m) {
		if (m.getSubject().toLowerCase().startsWith(SUBJECT_REQUEST_REGISTER)) {
			
			
			serviceDB.put(m.getTxt(), sender());
			sender().tell(Message.newBuilder()
					.setPerformative(PerformativeType.SUBSCRIBE)
					.setSource(self().path().toString())
					.setSubject(BasicActor.SUBJECT_QUERY_SUBSCRIBE_STATE).setTxt(self().toString()).build(), self());
			// watch may not be called twice
			getContext().unwatch(sender());
			getContext().watch(sender());
			Logging.getLogger(this).info("DF: registered: " + m.getTxt());

		} else if (m.getSubject().toLowerCase().startsWith(SUBJECT_REQUEST_UNREGISTER)) {
			if (serviceDB.containsKey(m.getTxt())) {
				// we are allow here any actor to unregister any actor!
				// a potential problem
				ActorRef dereg = serviceDB.remove(m.getTxt());
				if (dereg == null) {
					dereg = sender();
				}
				dereg.tell(
						Message.newBuilder().setPerformative(PerformativeType.UNSUBSCRIBE)
							.setSource(self().path().toString())
							.setSubject(BasicActor.SUBJECT_QUERY_SUBSCRIBE_STATE)
							.setTxt(self().toString()).build(),
						self());

				getContext().unwatch(dereg);
				resourceDB.remove(dereg.toString());

				Logging.getLogger(this).info("DF: UNregistered: " + m.getTxt());
			}
		}
	}

	/** informed about resource State - also when actor is dying */
	private void doInformResource(Resource m) {

		if (m.getState() != Resource.States.STATE_SHUTTING_DOWN && m.getState() != Resource.States.STATE_DEAD) {
			resourceDB.put(sender().toString(), m);
			Logging.getLogger(this).info("added / updated Resource: " + m);
		} else {
			resourceDB.remove(sender().toString());
			Logging.getLogger(this).info("removed Resource: " + m);
			String toremove = null;
			for (String key : serviceDB.keySet()) {
				if (serviceDB.get(key).equals(sender())) {
					// do not modify map during looping
					toremove = key;
					break;
				}
			}
			if (toremove != null)
				serviceDB.remove(toremove);
		}
	}

	/**
	 * Query for Actors
	 * we will receive a resource object with DirectoryFacilitator.SUBJECT_QUERY_RESOURCE txt == ActorRef.toString()
	 * we will receive an ActorRef with DirectoryFacilitator.SUBJECT_QUERY_ACTOR txt == service/actor name
	 * 
	 */
	private void doQuery(Message m) {

		Logging.getLogger(this).info("DF: " + m.toString());
		
		if (m.getSubject().equals(SUBJECT_QUERY_ACTOR) && serviceDB.containsKey(m.getTxt())) {
			sender().tell(serviceDB.get(m.getTxt()), self());
		} else if (m.getSubject().equals(SUBJECT_QUERY_RESOURCE) && resourceDB.containsKey(m.getTxt())) {
			sender().tell(resourceDB.get(m.getTxt()), self());
		} else if (m.getSubject().equals(SUBJECT_QUERY_ALL_ACTORS)) {
			for (ActorRef act : serviceDB.values())
				sender().tell(act, self());
		} else if (m.getSubject().equals(SUBJECT_QUERY_ALL_RESOURCES)) {
			for (Resource res : resourceDB.values())
				sender().tell(res, self());
		} else {
			sender().tell(Message.newBuilder()
					.setSource(self().path().toString())
					.setPerformative(PerformativeType.FAILURE)
					.setTxt(m.getTxt()).build(),
					sender());
		}
	}

	/**
	 * Unexpected - handle something here
	 * 
	 */
	private void doFailure(Message m) {
		Logging.getLogger(this).error(self().toString() + " received: " + m.toString());
	}

	private void watchedActorTerminated(Terminated m) {
		Logging.getLogger(this).error(self().toString() + " received: Actor Terminated: " + m.toString());
	}

}
