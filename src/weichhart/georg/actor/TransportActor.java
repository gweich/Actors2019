package weichhart.georg.actor;

import java.time.Duration;
import java.util.HashMap;

import akka.actor.Props;
import akka.actor.ReceiveTimeout;
import akka.event.Logging;
import akka.japi.pf.ReceiveBuilder;
import weichhart.georg.communication.PerformativeMessages.Message;
import weichhart.georg.communication.PerformativeMessages.Message.PerformativeType;
import weichhart.georg.communication.ResourceMessages.Resource;

public class TransportActor extends BasicActor {
	
	public static String Name = "Transporter";
	private static int NameNr = 0;
	
	HashMap<String,Resource> resourcePositionDB = new HashMap<>();
	HashMap<String,Resource> serviceDB = new HashMap<>();
	
	public static String SUBJECT_QUERY_START_STOP = "query-start-stop";


	public TransportActor(int posX, int posY) {
		super(Name+NameNr++, posX, posY);
	}
	
	@Override
	public void preStart() {
		super.preStart();
		
		// DF is not now available but we have to wait for some time
		 getContext().setReceiveTimeout(Duration.ofSeconds(2));

	}
	
	@Override
	public Receive createReceive() {
		return createBuilder().build();
	}
	
	public ReceiveBuilder createBuilder() {
		return super.createBuilder().match(Resource.class, r -> {
			resourcePositionDB.put(r.getPositionX()+":"+r.getPositionY(), r);
			serviceDB.put(r.getService(), r);
		}).match(Message.class, m -> m.getPerformative().equals(PerformativeType.QUERY_IF), 
				m -> doQuery(m))
		.match(Message.class, m -> {
			Logging.getLogger(this).info(" M:" +m); 
		}).match(ReceiveTimeout.class, rt -> {
			Logging.getLogger(this).info("timeout got DF:" + df);
			
			if(df!=null) {
				
				getContext().cancelReceiveTimeout();
				// cancel not working ??
				getContext().setReceiveTimeout(scala.concurrent.duration.Duration.Undefined());
				
				/// build up a db with to find the patch to services
				df.tell(Message.newBuilder()
						.setPerformative(PerformativeType.QUERY)
						.setSubject(DirectoryFacilitator.SUBJECT_QUERY_ALL_RESOURCES)
						.setSource(self().path().toString()).build()
						, self() );
			} else {
				// timeout will keep running
			}
		});
	}

	/** expecting a query with the txt of "x:y servicetarget" where x:y is the start position and service target is a name of the target actor */
	protected void doQuery(Message m) {
		
		Logging.getLogger(this).info("received Query" + m);
		
		if(m.getSubject().equals(SUBJECT_QUERY_START_STOP)) {
			Resource start = resourcePositionDB.get(m.getTxt().substring(0, m.getTxt().indexOf(' ')));
			Resource stop = serviceDB.get(m.getTxt().substring(m.getTxt().indexOf(' ')+1));
			
			Logging.getLogger(this).info("find path " + start + "  " + stop);
			
			getContext().actorOf(Props.create(TransportOptimizeActor.class, start, stop, resourcePositionDB, sender()));
		}
	}
	
}
