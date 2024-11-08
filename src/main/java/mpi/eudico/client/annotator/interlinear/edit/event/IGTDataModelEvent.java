package mpi.eudico.client.annotator.interlinear.edit.event;

import java.util.EventObject;
import mpi.eudico.client.annotator.interlinear.edit.model.IGTAnnotation;
import mpi.eudico.client.annotator.interlinear.edit.model.IGTDataModel;
import mpi.eudico.client.annotator.interlinear.edit.model.IGTTier;
/**
 * An IGTDataModelEvent object contains information about changes in a IGTDataModel.
 *
 * @author Han Sloetjes
 *
 */
@SuppressWarnings("serial")
public class IGTDataModelEvent extends EventObject {
	private IGTTier igtTier;
	private IGTAnnotation igtAnnotation;
	private ModelEventType eventType;

	/**
	 * Constructor accepting the source of the event as argument.
	 *
	 * @param source the source of the change event, the data model
	 */
	public IGTDataModelEvent(IGTDataModel source) {
		super(source);
	}

	/**
	 * Constructor accepting the source of the event as argument as well as the tier (row) that
	 * changed and the annotation.
	 *
	 * @param source the source of the change event, the data model
	 * @param tier the changed tier
	 * @param annotation the changed annotation
	 * @param type the event type
	 */
	public IGTDataModelEvent(IGTDataModel source, IGTTier tier, IGTAnnotation annotation, ModelEventType type) {
		super(source);
		igtTier = tier;
		igtAnnotation = annotation;
		eventType = type;
	}

	/**
	 * Returns the changed IGT tier.
	 *
	 * @return the igtTier
	 */
	public IGTTier getIgtTier() {
		return igtTier;
	}

	/**
	 * Returns the changed IGT annotation.
	 *
	 * @return the igtAnnotation
	 */
	public IGTAnnotation getIgtAnnotation() {
		return igtAnnotation;
	}

	/**
	 * Returns the type of model event.
	 *
	 * @return the eventType
	 */
	public ModelEventType getEventType() {
		return eventType;
	}

}
