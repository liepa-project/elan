package mpi.eudico.server.corpora.clomimpl.abstr;

import java.util.ArrayList;
import java.util.EventObject;
import java.util.Iterator;
import java.util.List;
import java.util.TreeSet;

import mpi.eudico.server.corpora.clom.Annotation;
import mpi.eudico.server.corpora.clom.Tier;
import mpi.eudico.server.corpora.clom.TimeSlot;
import mpi.eudico.server.corpora.clom.Transcription;
import mpi.eudico.server.corpora.clomimpl.type.Constraint;
import mpi.eudico.server.corpora.clomimpl.type.LinguisticType;
import mpi.eudico.server.corpora.event.ACMEditEvent;


/**
 * A time alignable annotation can have actual begin and end time values, 
 * stored in the begin and end {@link TimeSlot}. A {@link TimeSlot} can be
 * (temporarily) unaligned, it depends on the constraints of the tier the 
 * annotation is part of whether unaligned time slots are acceptable or not 
 * for an {@code AlignableAnnotation}.<p>
 * A fully instantiated {@code AlignableAnnotation} has a non-null begin and
 * end {@link TimeSlot}. At the moment single point annotations (only one time 
 * point, no duration) are not supported by the implementation of the model. 
 * A begin time equal to the end time is an error condition and used as a mark
 * for deletion of the annotation. 
 * 
 */
public class AlignableAnnotation extends AbstractAnnotation {
    /** the begin time slot */
    private TimeSlot beginTime;

    /** the end time slot */
    private TimeSlot endTime;

    /**
     * Creates a new AlignableAnnotation instance
     *
     * @param bts begin time slot
     * @param ets end time slot
     * @param theTier the tier this annotation is part of
     */
    public AlignableAnnotation(TimeSlot bts, TimeSlot ets, Tier theTier) {
        super();

        beginTime = bts;
        endTime = ets;
        this.setTier(theTier);

        // NOTE: this code assumes that parent Annotation already exists.
        // When reading in a document, this assumption does not always hold.
        // Therefore, when initially reading in a document, registerWithParent()
        // has to be called after all AlignableAnnotations are constructed.
        registerWithParent();
    }

    /**
     * Registers this AlignableAnnotation as a ParentAnnotationListener with
     * the parent annotation. (Is the counterpart of
     * RefAnnotation.addReference).
     */
    public void registerWithParent() {
        if (hasParentAnnotation()) {
            Annotation p = getParentAnnotation();

            if (p != null) {
                p.addParentAnnotationListener(this);
            }
        }
    }

    /**
     * Returns the begin {@link TimeSlot}.
     * 
     * @return the begin {@link TimeSlot}
     */
    public TimeSlot getBegin() {
        return beginTime;
    }

    /**
     * Sets the begin {@link TimeSlot}, should not be the same as the end
     * {@link TimeSlot}.
     *
     * @param theBegin the new begin {@link TimeSlot}
     */
    public void setBegin(TimeSlot theBegin) {
        beginTime = theBegin;
    }

    /**
     * Returns the end {@link TimeSlot}.
     * 
     * @return the end {@link TimeSlot}
     */
    public TimeSlot getEnd() {
        return endTime;
    }

    /**
     * Sets the end {@link TimeSlot}, should not be the same as the begin
     * {@link TimeSlot}.
     *
     * @param theEnd the new end {@link TimeSlot}
     */
    public void setEnd(TimeSlot theEnd) {
        endTime = theEnd;
    }

    /**
     * Updates the time interval represented by this annotation by setting a 
     * new begin and or end time. Changing the interval of an annotation, moving
     * it along the time line and/or changing its duration, can have an impact
     * on other annotations:<ul>
     * <li>annotations on the same tier can have their interval changed (to 
     * prevent overlapping annotations) or can be marked for deletion if they 
     * are fully covered by this annotation after the update
     * <li>annotations on dependent tiers might have their interval updated or
     * might be marked for deletion if they are outside of the new interval of
     * this annotation
     * </ul> 
     * After updating the interval a {@link ACMEditEvent#CHANGE_ANNOTATION_TIME}
     * event is posted.
     * 
     * Note: the implementation of this functionality could be moved to another
     * level e.g. the transcription, since it involves multiple tiers and 
     * constraints.
     * 
     * @param beginTime the new begin time in milliseconds
     * @param endTime the new end time in milliseconds
     */
    public void updateTimeInterval(long beginTime, long endTime) {
        long oldBegin = getBegin().getTime();
        long oldEnd = getEnd().getTime();

        // HS feb 2005: first check times in relation to the parent annotation's 
        // begin and end time   
        AlignableAnnotation parent = null;
        boolean isIncludedInAnn = false;
        
        if (this.hasParentAnnotation()) {
            parent = (AlignableAnnotation) this.getParentAnnotation();

            if ((beginTime >= parent.getEnd().getTime()) ||
                    (endTime <= parent.getBegin().getTime())) {
                return;
            }
            // HS June 2008 if parent's begin or end unaligned, don't apply real times
            if (!parent.getBegin().isTimeAligned() || !parent.getEnd().isTimeAligned()) {
            	return;
            }
            
            if (getBegin() == parent.getBegin() && getEnd() == parent.getEnd()) {
            	return;
            }
            
           isIncludedInAnn = (getTier().getLinguisticType().getConstraints().getStereoType() == Constraint.INCLUDED_IN);
           
            if (endTime >= parent.getEnd().getTime()) {
                endTime = parent.getEnd().getTime();
                // July 06: See comments at setBegin below.
                // this all needs to be revised...
                // setEnd(parent.getEnd());
            }

            if (beginTime <= parent.getBegin().getTime()) {
                beginTime = parent.getBegin().getTime();
                // July 06: to ensure that the annotations will be given the right time slot reference and 
                // overlapping annotations to be cleaned up, set the begin slot here. 
                // This doesn't work with the end slot though (due to the way overlapping annotations are 
                // forced out of the interval and later detached from the tier (see also TierImpl)
                // October 2015: check the type of the tier this annotation is on. In case of Included In
                // don't connect to time slot of the parent. (The Constraints of this tier can be assumed not to be null at this point.)
                if (!isIncludedInAnn) {
                	setBegin(parent.getBegin());
                }
            }            
        }

        TreeSet<AbstractAnnotation> connectedAnnots = new TreeSet<AbstractAnnotation>();
        TreeSet<TimeSlot> connectedTimeSlots = new TreeSet<TimeSlot>();

        ((((TierImpl) getTier()).getTranscription())).getConnectedAnnots(connectedAnnots,
            connectedTimeSlots, getBegin());

        List<AbstractAnnotation> connectedAnnotVector = new ArrayList<AbstractAnnotation>(connectedAnnots);
        TimeSlot[] graphEndpoints = ((TierImpl) getTier()).getGraphEndpoints(connectedAnnotVector);

        if (!((getBegin() == graphEndpoints[0]) && hasParentAnnotation() && !isIncludedInAnn) &&
                !(parent != null && getBegin() == parent.getBegin())) {
            getBegin().setTime(beginTime);
        }

        if (!((getEnd() == graphEndpoints[1]) && hasParentAnnotation() && !isIncludedInAnn) &&
                !(parent != null && getEnd() == parent.getEnd())) {
            getEnd().setTime(endTime);
        }

        // HB, 27-feb-02: correct potential time overlaps
        if (((((TierImpl) getTier()).getTranscription()).getTimeChangePropagationMode() == Transcription.BULLDOZER) &&
                (!hasParentAnnotation())) {
            ((TierImpl) getTier()).correctOverlapsByPushing(this, oldBegin,
                oldEnd);
            // the above takes care of annotations to the left or right of this annotation.
            // but in bulldozer mode depending time aligned annotations are not moved with the parent (this annotation), 
            // while that usually is the desired behavior
        } else if (((((TierImpl) getTier()).getTranscription()).getTimeChangePropagationMode() == Transcription.SHIFT) &&
                (!hasParentAnnotation())) {
            List<TimeSlot> fixedSlots = new ArrayList<TimeSlot>(connectedTimeSlots);
            (((TierImpl) getTier()).getTranscription()).correctOverlapsByShifting(this,
                fixedSlots, oldBegin, oldEnd);
            ((TierImpl) getTier()).correctTimeOverlaps(this);
        } else {
            ((TierImpl) getTier()).correctTimeOverlaps(this);
        }

        // HB, 10 apr 03, notify child annotations about time change
        notifyParentListeners();

        // HS jan 2005, (partially) unaligned children cannot mark themselves as 
        // deleted, take care of this here and now
        cleanUpUnalignedChildAnnotations();

        (((TierImpl) getTier()).getTranscription()).pruneAnnotations(this.getTier());

        // HS jan 05: preliminary fix for the case one annotation has been moved beyond another 
        // on the same tier. The order of the annotations in the TreeSet has become inconsistent
        boolean consistent = checkAnnotationOrderConsistency((TierImpl) this.getTier());

        if (!consistent) {
            // reuse the previously stored time slots
            resortAnnotationsAndSlots(connectedTimeSlots);
        }

        // end preliminary fix
        modified(ACMEditEvent.CHANGE_ANNOTATION_TIME, this);     
    }

    /**
     * Sometimes you want to make time-aligned children of annotations which
     * are alignable but not aligned. Force them to become aligned by setting
     * their begin and or end time to the virtual, interpolated time. This call
     * goes up the annotation hierarchy tree if necessary.
     */
    public void makeTimeAligned() {
        if (!getBegin().isTimeAligned() || !getEnd().isTimeAligned()) {
            if (hasParentAnnotation()) {
            	AlignableAnnotation parent = (AlignableAnnotation) this.getParentAnnotation();
                parent.makeTimeAligned();
            }
            
        	updateTimeInterval(getBeginTimeBoundary(), getEndTimeBoundary());
        }
    }

    /**
     * Returns a real or virtual time value in milliseconds.
     *
     * @return the actual begin time of this annotation's begin {@link TimeSlot}
     * or a virtual, interpolated time value if the begin {@link TimeSlot} is
     * unaligned
     */
    @Override
	public long getBeginTimeBoundary() {
        long beginBoundary = 0;

        if (beginTime.isTimeAligned()) {
            beginBoundary = beginTime.getTime();
        } else {
            // oct 04: the if part added for performance reasons
            if (((TimeSlotImpl) beginTime).getProposedTime() >= 0) {
                beginBoundary = ((TimeSlotImpl) beginTime).getProposedTime();
            } else {
                beginBoundary = ((TierImpl) getTier()).proposeTimeFor(beginTime);
            }
        }

        return beginBoundary;
    }

    /**
     * Returns a real or virtual time value in milliseconds.
     *
     * @return the actual end time of this annotation's end {@link TimeSlot}
     * or a virtual, interpolated time value if the end {@link TimeSlot} is
     * unaligned
     */
    @Override
	public long getEndTimeBoundary() {
        long endBoundary = getBeginTimeBoundary(); // media end time would be better

        //	long endBoundary = Long.MAX_VALUE;
        if (endTime.isTimeAligned()) {
            endBoundary = endTime.getTime();
        } else {
            // 
            //     endBoundary = ((TranscriptionImpl) (((TierImpl) getTier()).getParent())).getTimeOrder()
            //                    .proposeTimeFor(endTime);
            //	endBoundary = ((TierImpl) getTier()).proposeTimeFor(endTime);
            // oct 04: the if part added for performance reasons
            if (((TimeSlotImpl) endTime).getProposedTime() >= 0) {
                endBoundary = ((TimeSlotImpl) endTime).getProposedTime();
            } else {
                endBoundary = ((TierImpl) getTier()).proposeTimeFor(endTime);
            }
        }

        return endBoundary;
    }

    /**
     * Returns the begin time if the begin time slot is time aligned or a new
     * proposed time when the slot is unaligned.  Note: Oct. '04 addition
     * related to performance of unaligned slots.
     *
     * @return the real or virtual begin time boundary
     */
    public long calculateBeginTime() {
        if (beginTime.isTimeAligned()) {
            return beginTime.getTime();
        } else {
            return ((TierImpl) getTier()).proposeTimeFor(beginTime);
        }
    }

    /**
     * Returns the end time if the end time slot is time aligned or a new
     * proposed time when the slot is unaligned.  Note: Oct. '04 addition
     * related to performance of unaligned slots.
     *
     * @return the real or virtual end time boundary
     */
    public long calculateEndTime() {
        if (endTime.isTimeAligned()) {
            return endTime.getTime();
        } else {
            return ((TierImpl) getTier()).proposeTimeFor(endTime);
        }
    }

    /*
       // Comparable interface method
       public int compareTo(Object obj) {
           return beginTime.compareTo(((AlignableAnnotation) obj).getBegin());
       } */

    // ParentAnnotationListener implementation
    /**
     * If the parent annotation has been marked for deletion, this annotation
     * will mark itself for deletion. If the time interval changed, this 
     * annotation might need to update its interval or mark itself for deletion.
     * 
     * @param e the {@link EventObject} only contains the source of the event,
     * the parent annotation, not what changed
     */
    @Override
	public void parentAnnotationChanged(EventObject e) {
        if (e.getSource() instanceof AlignableAnnotation) {
            if (((Annotation) e.getSource()).isMarkedDeleted()) {
                ((AlignableAnnotation) e.getSource()).removeParentAnnotationListener(this);
                markDeleted(true);
            } else { // HB, 7 may 03, adjust to parent time alignment changes

                // force times within parent's time interval
                long parentBegin = ((AlignableAnnotation) e.getSource()).getBegin()
                                    .getTime();
                long parentEnd = ((AlignableAnnotation) e.getSource()).getEnd()
                                  .getTime();

                if (beginTime.isTimeAligned()) {
                    if (beginTime.getTime() < parentBegin) {
                        beginTime.setTime(parentBegin);
                    }

                    if (beginTime.getTime() > parentEnd) {
                        beginTime.setTime(parentEnd);
                    }
                }

                if (endTime.isTimeAligned()) {
                    if (endTime.getTime() > parentEnd) {
                        endTime.setTime(parentEnd);
                    }

                    if (endTime.getTime() < parentBegin) {
                        endTime.setTime(parentBegin);
                    }
                }

                // HB, 10 apr 03, adjust to parent time alignment changes
                if ((beginTime.isTimeAligned()) && (endTime.isTimeAligned()) &&
                        (beginTime.getTime() >= endTime.getTime())) {
                    ((AlignableAnnotation) e.getSource()).removeParentAnnotationListener(this);
                    markDeleted(true);
                }
            }
        }
    }

    /**
     * Checks if this AlignableAnnotation has a parent Annotation on basis of
     * the assumption that Constraints are always met (e.g. if the annotation
     * is part of a time subdivision of some other annotation, it is assumed
     * that this annotation exists.
     *
     * @return true if the tier this annotation is part of has (or should have)
     * a parent tier, based the {@link Constraint}s of the tier's 
     * {@link LinguisticType}
     */
    @Override
	public boolean hasParentAnnotation() {
        boolean hasParent = false;

        LinguisticType lt = ((TierImpl) getTier()).getLinguisticType();

        if (lt != null) {
            Constraint c = lt.getConstraints();

            if (c != null) {
                if (c.getStereoType() == Constraint.TIME_SUBDIVISION ||
                        c.getStereoType() == Constraint.INCLUDED_IN) {
                    hasParent = true;
                }
            }
        }

        return hasParent;
    }

    /**
     * Parent-child relationship for AlignableAnnotations is defined by 1.
     * sharing of TimeSlots between annotations. 2. parent-child relationship
     * between their tiers This method determines the parent annotation on
     * basis of this. Return null if no parent.
     *
     * @return 
     */

    /*    public Annotation getParentAnnotation() {
       AlignableAnnotation parent = null;
       TierImpl parentTier = (TierImpl) ((TierImpl) getTier()).getParentTier();
       if (parentTier != null) {

               //        TimeOrder to = ((TranscriptionImpl)(((TierImpl) getTier()).getParent())).getTimeOrder();
               TreeSet connectedAnnots = new TreeSet();
               TreeSet connectedTimeSlots = new TreeSet();
               ((TranscriptionImpl) (parentTier.getParent())).getConnectedAnnots(connectedAnnots,
                   connectedTimeSlots, getBegin());
               Iterator annIter = parentTier.getAnnotations(null).iterator();
    
               // make copy of TreeSet. For some reason I don't understand the treeset sometimes
               // incorrectly returns false when 'contains' is called.
               List connectedAnnotVector = new List(connectedAnnots);
    
               while (annIter.hasNext()) {
                   AlignableAnnotation a = (AlignableAnnotation) annIter.next();
                   if (connectedAnnotVector.contains(a)) {
                       TreeSet subtreeAnnots = new TreeSet();
                       ((TranscriptionImpl) (parentTier.getParent())).getConnectedSubtree(subtreeAnnots,
                           a.getBegin(), a.getEnd());
    
                          Vector subtreeVector = new Vector(subtreeAnnots);
                          if (subtreeVector.contains(this)) {
                           parent = a;
                           break;
                          }
                   }
               }

       }
       return parent;
       }
     */

    /**
     * Parent-child relationship for AlignableAnnotations is defined by <ul>
     * <li>1. sharing of TimeSlots between annotations. 
     * <li>2. parent-child relationship between their tiers.
     * </ul> 
     * This method determines the parent annotation on the basis of this and 
     * returns null if there is no parent.
     *
     * @return the parent annotation or null
     */
    @Override
	public Annotation getParentAnnotation() {
        AlignableAnnotation parent = null;

        TierImpl parentTier = ((TierImpl) getTier()).getParentTier();

        if (parentTier != null) {
            final int stereoType = ((TierImpl) getTier()).getLinguisticType().getConstraints().getStereoType();
			if (stereoType == Constraint.TIME_SUBDIVISION) {
	            TimeSlot chainBegin = ((TierImpl) this.getTier()).getBeginSlotOfChain(getBegin(),
	                    false);
	
	            List<Annotation> candidates = parentTier.getAnnotationsUsingTimeSlot(chainBegin);
	
	            for (int i = 0; i < candidates.size(); i++) {
	                AlignableAnnotation a = (AlignableAnnotation) candidates.get(i);
	
	                if (a.getBegin() == chainBegin) {
	                    parent = a;
	
	                    break;
	                }
	            }
            } else if(stereoType == Constraint.INCLUDED_IN) {
                parent = (AlignableAnnotation) parentTier.getAnnotationAtTime(getBegin().getTime());
            }
        }
        
        return parent;
    }

    /**
     * Checks whether this annotation is an ancestor annotation of the specified
     * annotation. The check is based on presence in the parent annotation 
     * listener list. 
     * 
     * @param aa the annotation
     * @return true if this annotation is an ancestor of aa, false otherwise
     */
    public boolean isAncestorOf(AlignableAnnotation aa) {
        if (aa == null) {
            return false;
        }
        if (getParentListeners().contains(aa)) {
            return true;
        } else {
            List<Annotation> pl = getParentListeners();
            AlignableAnnotation chan;
            for (int i = 0; i < pl.size(); i++) {
                if (pl.get(i) instanceof AlignableAnnotation) {
                    chan = (AlignableAnnotation) pl.get(i);
                    if (chan.isAncestorOf(aa)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
    
    /**
     * When updateTimeInterval() has been called notifyParentListeners (and
     * thus parentAnnotationChanged()) takes care of marking annotations for
     * deletion that have a begin- and end-timeslot that are time aligned.
     * (Partially) unaligned annotations are not marked for deletion; this is
     * therefore done here.
     *
     * @since jan 2005
     */
    private void cleanUpUnalignedChildAnnotations() {
        List<Annotation> l = this.getParentListeners();
        List<AlignableAnnotation> yetToBeDeleted = new ArrayList<AlignableAnnotation>();

        for (int i = 0; i < l.size(); i++) {
            Annotation a = l.get(i);

            if (a instanceof AlignableAnnotation) {
                AlignableAnnotation aa = (AlignableAnnotation) a;

                if (!aa.getBegin().isTimeAligned() ||
                        !aa.getEnd().isTimeAligned()) {
                    if (aa.calculateBeginTime() == aa.calculateEndTime()) {
                        // do not call markDeleted here, because that would change the
                        // array of parent listeners and influence the operation
                        yetToBeDeleted.add(aa);
                    }
                }
            }
        }

        for (int j = 0; j < yetToBeDeleted.size(); j++) {
            yetToBeDeleted.get(j).markDeleted(true);
        }
    }

    /**
     * When an annotation has been moved beyond another annotation on the same
     * tier  the annotations treeset has to be resorted (also on depending
     * tiers).  Unaligned TimeSlots have not been repositioned, this is done
     * here also.
     *
     * @param oldConnectedTimeSlots the set of previously connected time slots
     */
    private void resortAnnotationsAndSlots(TreeSet<TimeSlot> oldConnectedTimeSlots) {
        ((TierImpl) this.getTier()).resortAnnotations();

        // unaligned TimeSlots are not automatically repositioned, do it here
        // undesirable, temporary solution
        TimeOrderImpl timeOrder = (TimeOrderImpl) (((TierImpl) getTier()).getTranscription()).getTimeOrder();

        // use the previously collected time slots
        List<TimeSlot> slots = new ArrayList<TimeSlot>(oldConnectedTimeSlots);
        Iterator<TimeSlot> elements;

        for (int i = slots.size() - 1; i >= 0; i--) {
            TimeSlot sl = slots.get(i);
            elements = timeOrder.iterator();

            boolean stillPresent = false;

            while (elements.hasNext()) {
                if (elements.next() == sl) {
                    stillPresent = true;

                    break;
                }
            }

            // remove slots that have been pruned from the TimeOrder
            if (!stillPresent) {
                slots.remove(i);
            }
        }

        if (!slots.contains(getBegin())) {
            slots.add(0, getBegin());
        }

        if (!slots.contains(getEnd())) {
            slots.add(getEnd());
        }

        // reposition the unaligned time slots
        for (int i = 0; i < (slots.size() - 2); i++) {
            TimeSlot t1 = slots.get(i);
            TimeSlot t2 = slots.get(i + 1);
            TimeSlot t3 = slots.get(i + 2);

            if (!t2.isTimeAligned()) {
                int index1 = t1.getIndex();
                int index2 = t2.getIndex();
                int index3 = t3.getIndex();

                //System.out.println("t1: " + index1 + " t2:" + index2 + " t3: " + index3);
                if (index2 < index1) {
                    timeOrder.removeTimeSlot(t2);

                    if (index2 > index3) {
                        timeOrder.insertTimeSlot(t2, t1, null);
                    } else {
                        timeOrder.insertTimeSlot(t2, t1, t3);
                    }
                } else if (index2 > index1) {
                    timeOrder.removeTimeSlot(t2);
                    timeOrder.insertTimeSlot(t2, t1, null);
                } else if (index2 > index3) {
                    timeOrder.removeTimeSlot(t2);

                    if (index2 > index1) {
                        timeOrder.insertTimeSlot(t2, t1, null);
                    } else {
                        timeOrder.insertTimeSlot(t2, t1, t3);
                    }
                }
            }
        }

        List<TierImpl> depTiers = ((TierImpl) this.getTier()).getDependentTiers();

        for (int i = 0; i < depTiers.size(); i++) {
            depTiers.get(i).resortAnnotations();
        }
    }

    /**
     * Checks the consistency of the annotation ordering of the specified tiers
     * and it's depending tiers.
     *
     * @param fromTier the 'root' tier of the tiers to check
     *
     * @return true if all annotations on all relevant tiers are ordered
     *         correctly, false otherwise
     */
    private boolean checkAnnotationOrderConsistency(TierImpl fromTier) {
        boolean consistent = fromTier.checkAnnotationOrderConsistency();

        if (!consistent) {
            return consistent;
        } else {
            List<TierImpl> depTiers = fromTier.getDependentTiers();

            for (int i = 0; i < depTiers.size(); i++) {
                consistent = depTiers.get(i).checkAnnotationOrderConsistency();

                if (!consistent) {
                    return consistent;
                }
            }
        }

        // if we get here all relevant tiers are consistent
        return consistent;
    }
}
