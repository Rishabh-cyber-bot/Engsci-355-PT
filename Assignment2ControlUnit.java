// Importing packages
package labs;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import com.jaamsim.input.ExpError;
import com.jaamsim.input.ExpResult;
import com.jaamsim.units.DimensionlessUnit;
import hccm.events.LogicEvent;
import hccm.activities.ProcessActivity;
import hccm.controlunits.ControlUnit;
import hccm.entities.ActiveEntity;

// Making a function containing the triggers 
public class Assignment2ControlUnit extends ControlUnit {
	
	public void DispatchOrderlies(List<ActiveEntity> ents, double simTime) {
	
		ArrayList<ActiveEntity> idleOrderlies1 = this.getEntitiesInActivity("Orderly", "PatientTransit.wait-task-orderly",simTime);
		
		ArrayList<ActiveEntity> idlePatients = this.getEntitiesInActivity("Patient", "PatientTransit.WaitForAssignment",simTime);
		
		ArrayList<ActiveEntity> idleOrderlies2 = this.getEntitiesInActivity("Orderly", "PatientTransit.WaitforDropoff",simTime);
		
		// Orderlies either waiting for task or waiting for drop off basically appending to the end of the list
		idleOrderlies1.addAll(idleOrderlies2);
		
		// comparing the activities
		ActivityStartCompare actSartComp = this.new ActivityStartCompare();	
		
		// If there is a patient and there are orderlies available
			if (idlePatients.size()> 0 && idleOrderlies1.size() > 0) {
				
				// Total count of patients
				int np = idlePatients.size();
				
				// Total count of orderlies
				int no = idleOrderlies1.size();
			
			// if there is a patient waiting and orderly is ready 
			while((np>0)&&(no>0)) {
				
				// Finding patient and orderly with the lowest current start and sorting them in order
				Collections.sort(idleOrderlies1, actSartComp);
				Collections.sort(idlePatients, actSartComp);
				
				// Obtaining the first patient and orderlies waiting
				ActiveEntity Patient = idlePatients.get(0);
				ActiveEntity Orderly = idleOrderlies1.get(0);
				
				// changing the attribute
				ExpResult eR1 = ExpResult.makeNumResult(Orderly.getOutputHandle("ID").getValueAsDouble(simTime, -1), DimensionlessUnit.class);
				ExpResult eR2 = ExpResult.makeNumResult(Patient.getOutputHandle("ID").getValueAsDouble(simTime, -1), DimensionlessUnit.class);
			    try {
			      Patient.setAttribute("AssignedOrderlyID", null, eR1);
			      Orderly.setAttribute("AssignedPatientID", null, eR2);
			        } catch (ExpError e) {
			      // TODO Auto-generated catch block
			      e.printStackTrace();
			    }
                
			    // remove patient and orderly from their list respectively
                idlePatients.remove(Patient);
                idleOrderlies1.remove(Orderly);
                
                // Subtracting one from the total count of both the patients and the orderlies since they have left the 'queue' 
				np = np - 1;
				no = no - 1;
				
				// Stores them in an array named as participants
				ArrayList<ActiveEntity> participants = new ArrayList<ActiveEntity>(Arrays.asList(Patient,Orderly));
				
				// Finishing the current activity for the patient and the orderly
				Patient.getCurrentActivity().finish(Patient.asList());
				Orderly.getCurrentActivity().finish(Orderly.asList());
				
				// Starting the activity for orderly travel to patient
				((ProcessActivity) this.getSubmodelEntity("OrderlyTravelToPatient")).start(participants);
			}
			}
	}

	// Second Logic 
	public void OnStartWaitAtDropoff(List<ActiveEntity> ents, double simTime) {
		
		// Creating a logic event 
		LogicEvent le = (LogicEvent) getSubmodelEntity("CheckWaitDropOff");
		le.scheduleEvent(ents, simTime + 10*60); // Used a constant value of time
			this.DispatchOrderlies(ents, simTime+10*60); // Calling Dispatch orderlies function
			
	}
      
	// Third Logic
      public void OnCheckWaitDropoff(List<ActiveEntity> ents, double simTime) {
    	  
    	  // Getting the first entry of the orderly active entity
    	  ActiveEntity Orderly = ents.get(0);
    	  
    	  // Using the if statement; if the travel to base scheduled is 1  
             if (Orderly.getOutputHandle("TTBScheduled").getValueAsDouble(simTime, -1)==1) {
            	 
             }
             		  // Transition 7 with orderly
                      Orderly.getCurrentActivity().finish(Orderly.asList());
                      // 
                      ArrayList<ActiveEntity> participants = new ArrayList<ActiveEntity>(Arrays.asList(Orderly));
                      // Starting travel to base process activity
                      ((ProcessActivity)this.getSubmodelEntity("TravelToBase")).start(participants);
      }
}