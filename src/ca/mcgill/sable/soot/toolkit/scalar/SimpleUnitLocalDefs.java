/* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
 * Jimple, a 3-address code Java(TM) bytecode representation.        *
 * Copyright (C) 1997, 1998 Raja Vallee-Rai (kor@sable.mcgill.ca)    *
 * All rights reserved.                                              *
 *                                                                   *
 * This work was done as a project of the Sable Research Group,      *
 * School of Computer Science, McGill University, Canada             *
 * (http://www.sable.mcgill.ca/).  It is understood that any         *
 * modification not identified as such is not covered by the         *
 * preceding statement.                                              *
 *                                                                   *
 * This work is free software; you can redistribute it and/or        *
 * modify it under the terms of the GNU Library General Public       *
 * License as published by the Free Software Foundation; either      *
 * version 2 of the License, or (at your option) any later version.  *
 *                                                                   *
 * This work is distributed in the hope that it will be useful,      *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of    *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU *
 * Library General Public License for more details.                  *
 *                                                                   *
 * You should have received a copy of the GNU Library General Public *
 * License along with this library; if not, write to the             *
 * Free Software Foundation, Inc., 59 Temple Place - Suite 330,      *
 * Boston, MA  02111-1307, USA.                                      *
 *                                                                   *
 * Java is a trademark of Sun Microsystems, Inc.                     *
 *                                                                   *
 * To submit a bug report, send a comment, or get the latest news on *
 * this project and other Sable Research Group projects, please      *
 * visit the web site: http://www.sable.mcgill.ca/                   *
 * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */

/*
 Reference Version
 -----------------
 This is the latest official version on which this file is based.

 Change History
 --------------
 A) Notes:

 Please use the following template.  Most recent changes should
 appear at the top of the list.

 - Modified on [date (March 1, 1900)] by [name]. [(*) if appropriate]
   [description of modification].

 Any Modification flagged with "(*)" was done as a project of the
 Sable Research Group, School of Computer Science,
 McGill University, Canada (http://www.sable.mcgill.ca/).

 You should add your copyright, using the following template, at
 the top of this file, along with other copyrights.

 *                                                                   *
 * Modifications by [name] are                                       *
 * Copyright (C) [year(s)] [your name (or company)].  All rights     *
 * reserved.                                                         *
 *                                                                   *

 B) Changes:
 
 - Modified on March 13, 1999 by Raja Vallee-Rai (rvalleerai@sable.mcgill.ca) (*)
   Re-organized the timers.

 - Modified on November 2, 1998 by Raja Vallee-Rai (kor@sable.mcgill.ca) (*)
   Repackaged all source files and performed extensive modifications.
   First initial release of Soot.

 - Modified on 23-Jul-1998 by Raja Vallee-Rai (kor@sable.mcgill.ca). (*)
   Renamed the uses of Hashtable to HashMap.

 - Modified on 15-Jun-1998 by Raja Vallee-Rai (kor@sable.mcgill.ca). (*)
   First internal release (Version 0.1).
*/

package ca.mcgill.sable.soot.toolkit.scalar;

import ca.mcgill.sable.soot.jimple.*;
import ca.mcgill.sable.soot.*;
import ca.mcgill.sable.util.*;
import java.util.*;


// FSet version

public class SimpleUnitLocalDefs implements UnitLocalDefs
{
    Map localUnitPairToDefs;

    public SimpleUnitLocalDefs(CompleteUnitGraph g)
    {
        if(Main.isProfilingOptimization)
            Main.defsTimer.start();
        
        if(Main.isVerbose)
            System.out.println("[" + g.getBody().getMethod().getName() +
			       "]     Constructing SimpleUnitLocalDefs...");
    
        UnitLocalDefsFlowAnalysis analysis = new UnitLocalDefsFlowAnalysis(g);
        
        if(Main.isProfilingOptimization)
            Main.defsPostTimer.start();

        // Build localUnitPairToDefs map
        {
            Iterator unitIt = g.iterator();

            localUnitPairToDefs = new HashMap(g.size() * 2 + 1, 0.7f);

            while(unitIt.hasNext())
		{
		    Unit s = (Unit) unitIt.next();

		    Iterator boxIt = s.getUseBoxes().iterator();

		    while(boxIt.hasNext())
			{
			    ValueBox box = (ValueBox) boxIt.next();

			    if(box.getValue() instanceof Local)
				{
				    Local l = (Local) box.getValue();
				    LocalUnitPair pair = new LocalUnitPair(l, s);

				    if(!localUnitPairToDefs.containsKey(pair))
					{
					    IntPair intPair = (IntPair) analysis.localToIntPair.get(l);
					    BoundedFlowSet value = (BoundedFlowSet) analysis.getFlowBeforeUnit(s);

					    List unitLocalDefs = value.toList(intPair.op1, intPair.op2);

					    localUnitPairToDefs.put(pair, Collections.unmodifiableList(unitLocalDefs));
					}
				}
			}
		}
        }

        if(Main.isProfilingOptimization)
            Main.defsPostTimer.end();
                
        if(Main.isProfilingOptimization)
            Main.defsTimer.end();
    }

    public List getDefsOfAt(Local l, Unit s)
    {
        LocalUnitPair pair = new LocalUnitPair(l, s);

        List toReturn = (List) localUnitPairToDefs.get(pair);
        
        if(toReturn == null)
            throw new RuntimeException("Illegal UnitLocalDefs query; local " + l + " has no definition at " + 
				       ((ToBriefString) s).toBriefString());
        
        return toReturn;
    }

    /*
      public List getDefsOfBefore(Local l, Unit s)
      {
      IntPair pair = (IntPair) analysis.localToIntPair.get(l);
      FSet value = (FSet) analysis.getValueBeforeUnit(s);

      List unitLocalDefs = value.toList(pair.op1, pair.op2);

      return unitLocalDefs;
      }*/

    /*
      Object[] elements = ((FSet) analysis.getValueBeforeUnit(s)).toArray();
      List listOfDefs = new LinkedList();

      // Extract those defs which correspond to this local
      {
      for(int i = 0; i < elements.length; i++)
      {
      DefinitionUnit d = (DefinitionUnit) elements[i];

      if(d.getLeftOp() == l)
      listOfDefs.add(d);
      }
      }

      // Convert the array so that it's of an appropriate form
      {
      Object[] objects = listOfDefs.toArray();
      DefinitionUnit[] defs = new DefinitionUnit[objects.length];

      for(int i = 0; i < defs.length; i++)
      defs[i] = (DefinitionUnit) objects[i];

      return defs;
      }

      }
      }
    */

    /*
      public DefinitionUnit[] getDefsOfAfter(Local l, Unit s)
      {
      Object[] elements = ((FSet) analysis.getValueAfterUnit(s)).toArray();
      List listOfDefs = new LinkedList();

      // Extract those defs which correspond to this local
      {
      for(int i = 0; i < elements.length; i++)
      {
      DefinitionUnit d = (DefinitionUnit) elements[i];

      if(d.getLeftOp() == l)
      listOfDefs.add(d);
      }
      }

      // Convert the array so that it's of an appropriate form
      {
      Object[] objects = listOfDefs.toArray();
      DefinitionUnit[] defs = new DefinitionUnit[objects.length];

      for(int i = 0; i < defs.length; i++)
      defs[i] = (DefinitionUnit) objects[i];

      return defs;
      }
      }

      public DefinitionUnit[] getDefsBefore(Unit s)
      {
      Object[] elements = ((FSet) analysis.getValueBeforeUnit(s)).toArray();
      DefinitionUnit[] defs = new DefinitionUnit[elements.length];

      for(int i = 0; i < elements.length; i++)
      defs[i] = (DefinitionUnit) elements[i];

      return defs;
      }

      public DefinitionUnit[] getDefsAfter(Unit s)
      {
      Object[] elements = ((FSet) analysis.getValueAfterUnit(s)).toArray();
      DefinitionUnit[] defs = new DefinitionUnit[elements.length];

      for(int i = 0; i < elements.length; i++)
      defs[i] = (DefinitionUnit) elements[i];

      return defs;
      }
    */
}

class IntPair
{
    int op1, op2;

    public IntPair(int op1, int op2)
    {
        this.op1 = op1;
        this.op2 = op2;
    }

}

class UnitLocalDefsFlowAnalysis extends ForwardUnitFlowAnalysis
{
    FlowSet emptySet;
    Map localToPreserveSet;
    Map localToIntPair;

    public UnitLocalDefsFlowAnalysis(UnitGraph g)
    {
        super(g);

        Object[] defs;
        FlowUniverse defUniverse;

        if(Main.isProfilingOptimization)
	    Main.defsSetupTimer.start();

        // Create a list of all the definitions and group defs of the same local together
        {
            Map localToDefList = new HashMap(g.getBody().getLocalCount() * 2 + 1, 0.7f);

            // Initialize the set of defs for each local to empty
            {
                Iterator localIt = g.getBody().getLocals().iterator();

                while(localIt.hasNext())
		    {
			Local l = (Local) localIt.next();

			localToDefList.put(l, new ArrayList());
		    }
            }

            // Fill the sets up
            {
                Iterator it = g.iterator();

                while(it.hasNext())
		    {
			Unit s = (Unit) it.next();

		    
			List defBoxes = s.getDefBoxes();
			if(!defBoxes.isEmpty()) {
			    if(!(defBoxes.size() ==1)) 
				throw new RuntimeException("UnitFastColorer: invalid number of def boxes");
			    
			    if(((ValueBox)defBoxes.get(0)).getValue() instanceof Local) {
				Local defLocal = (Local) ((ValueBox)defBoxes.get(0)).getValue();
				List l = (List) localToDefList.get(defLocal);
                            
				if(l == null)
				    throw new RuntimeException("local " + defLocal + " is used but not declared!");
				else
				    l.add(s);
			    }
			}
		    
		    }
            }

            // Generate the list & localToIntPair
            {
                Iterator it = g.getBody().getLocals().iterator();
                List defList = new LinkedList();

                int startPos = 0;

                localToIntPair = new HashMap(g.getBody().getLocalCount() * 2 + 1, 0.7f);

                // For every local, add all its defs
                {
                    while(it.hasNext())
			{
			    Local l = (Local) it.next();
			    Iterator jt = ((List) localToDefList.get(l)).iterator();

			    int endPos = startPos - 1;

			    while(jt.hasNext())
				{
				    defList.add(jt.next());
				    endPos++;
				}

			    localToIntPair.put(l, new IntPair(startPos, endPos));

			    // System.out.println(startPos + ":" + endPos);

			    startPos = endPos + 1;
			}
                }

                defs = defList.toArray();
                defUniverse = new FlowUniverse(defs);
            }
        }

        emptySet = new ArrayPackedSet(defUniverse);

        // Create the preserve sets for each local.
        {
            Map localToKillSet = new HashMap(g.getBody().getLocalCount() * 2 + 1, 0.7f);
            localToPreserveSet = new HashMap(g.getBody().getLocalCount() * 2 + 1, 0.7f);

            Chain locals = g.getBody().getLocals();

            // Initialize to empty set
            {
                Iterator localIt = locals.iterator();

                while(localIt.hasNext())
		    {
			Local l = (Local) localIt.next();

			localToKillSet.put(l, emptySet.clone());
		    }
            }

            // Add every definition of this local
	    for(int i = 0; i < defs.length; i++)
                {
                    Unit s = (Unit) defs[i];
		    
		    List defBoxes = s.getDefBoxes();
		    if(!(defBoxes.size() ==1)) 
			throw new RuntimeException("SimpleUnitLocalDefs: invalid number of def boxes");
			    
		    if(((ValueBox)defBoxes.get(0)).getValue() instanceof Local) {
			Local defLocal = (Local) ((ValueBox)defBoxes.get(0)).getValue();
			BoundedFlowSet killSet = (BoundedFlowSet) localToKillSet.get(defLocal);
			killSet.add(s, killSet);
			
		    }
		}
	    
            // Store complement
            {
                Iterator localIt = locals.iterator();

                while(localIt.hasNext())
		    {
			Local l = (Local) localIt.next();

			BoundedFlowSet killSet = (BoundedFlowSet) localToKillSet.get(l);

			killSet.complement(killSet);

			localToPreserveSet.put(l, killSet);
		    }
            }
        }

        if(Main.isProfilingOptimization)
	    Main.defsSetupTimer.end();

        if(Main.isProfilingOptimization)
	    Main.defsAnalysisTimer.start();

        doAnalysis();
        
        if(Main.isProfilingOptimization)
	    Main.defsAnalysisTimer.end();
    }
    
    protected Object newInitialFlow()
    {
        return emptySet.clone();
}

    protected void flowThrough(Object inValue, Unit unit, Object outValue)
    {
	FlowSet in = (FlowSet) inValue, out = (FlowSet) outValue;
	

	List defBoxes = unit.getDefBoxes();
	if(!defBoxes.isEmpty()) {
	    if(!(defBoxes.size() ==1)) 
		throw new RuntimeException("UnitFastColorer: invalid number of def boxes");
			  
	    Value value = ((ValueBox)defBoxes.get(0)).getValue();
	    if(value  instanceof Local) {
		Local defLocal = (Local) value;
	    
		// Perform kill on value
		in.intersection((FlowSet) localToPreserveSet.get(defLocal), out);

		// Perform generation
		out.add(unit, out);
	    } else { 
		in.copy(out);
		return;
	    }


	

	}
	else
	    in.copy(out);
    }


protected void copy(Object source, Object dest)
{
    FlowSet sourceSet = (FlowSet) source,
	destSet = (FlowSet) dest;
            
    sourceSet.copy(destSet);
}

protected void merge(Object in1, Object in2, Object out)
{
    FlowSet inSet1 = (FlowSet) in1,
	inSet2 = (FlowSet) in2;

    FlowSet outSet = (FlowSet) out;

    inSet1.union(inSet2, outSet);
}
}
