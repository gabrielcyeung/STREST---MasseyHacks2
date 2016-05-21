# SDK Notes
Notes of important classes and methods that may be useful later.

## Muse
- abstract void com.choosemuse.libmuse.MuseManager.setMuseListener	(	MuseListener 	listener	)	
- abstract void com.choosemuse.libmuse.MuseManager.startListening	(		)	
- abstract void com.choosemuse.libmuse.MuseManager.stopListening	(		)	
- boolean com.choosemuse.libmuse.MuseManagerAndroid.isBluetoothEnabled	(		)	
- void com.choosemuse.libmuse.MuseManagerAndroid.setContext	(	Context 	context	)	
- void com.choosemuse.libmuse.MuseManagerAndroid.setMuseListener	(	MuseListener 	listener	)	
- void com.choosemuse.libmuse.MuseManagerAndroid.startListening	(		)	
- void com.choosemuse.libmuse.MuseManagerAndroid.stopListening	(		)	
- abstract void com.choosemuse.libmuse.LogManager.setLogListener	(	LogListener 	listener	)	
- com.choosemuse.libmuse.MessageType.CALM_ALG
- com.choosemuse.libmuse.MessageType.CALM_APP
- abstract void com.choosemuse.libmuse.Muse.registerConnectionListener	(	MuseConnectionListener 	listener	)
- abstract void com.choosemuse.libmuse.Muse.registerDataListener	(	MuseDataListener 	listener, MuseDataPacketType 	type )
- abstract void com.choosemuse.libmuse.Muse.registerErrorListener	(	MuseErrorListener 	listener	)	
- abstract void com.choosemuse.libmuse.Muse.unregisterAllListeners	(		)	
- boolean com.choosemuse.libmuse.MuseArtifactPacket.getBlink	(		)	
- boolean com.choosemuse.libmuse.MuseArtifactPacket.getHeadbandOn	(		)	
- boolean com.choosemuse.libmuse.MuseArtifactPacket.getJawClench	(		)	
- abstract void com.choosemuse.libmuse.MuseDataListener.receiveMuseArtifactPacket	(	MuseArtifactPacket 	packet,
Muse 	muse 
)	
- abstract void com.choosemuse.libmuse.MuseDataListener.receiveMuseDataPacket	(	MuseDataPacket 	packet,
Muse 	muse 
)	
- com.choosemuse.libmuse.MuseDataPacket
- abstract void com.choosemuse.libmuse.MuseErrorListener.receiveError	(	Error 	error	)	
- abstract void com.choosemuse.libmuse.MuseListener.museListChanged	(		)	
- com.choosemuse.libmuse.MuseLog
