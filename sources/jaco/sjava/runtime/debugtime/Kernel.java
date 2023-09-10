/**
 * Monitor that can be used for debugging or analysis purposes:  Kernel.java
 */

package sjava;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.util.*;
import java.lang.reflect.*;

class Rendezvous {
    Select caller;
    int callerI;
    Select acceptor;
    int acceptorI;
    public Rendezvous (Select caller, int callerI, Select acceptor, int acceptorI) {
	this.caller = caller;
	this.callerI = callerI;
	this.acceptor = acceptor;
	this.acceptorI = acceptorI;
    }
}

public class Kernel implements Runnable{


    static public Kernel kernel = null;
    java.util.List selectList = new LinkedList(); // queue of selects
    LinkedList rendezvousList = new LinkedList(); // queues of ready rendezvous

    AFrame fram;
    JTextField txt;
    JButton button;
    JTextArea area;
    boolean goShow = false, goStart = false, goPrint = false, goHelp = false;

    public Kernel () {
	// create a local display
	fram = new AFrame("            DEBUGGER",50,400);  // uses a grid bag
//  	txt = new JTextField(30);
//  	fram.add(txt, 0, 0, 3, 1);
	button = new JButton("Highlight a fragment of the rendezvous you "+
			     "want to resume and then click here !");
	button.addActionListener ( new ActionListener() {
		public void actionPerformed (ActionEvent e) {
		    goStart = true;
		    notifyKernel();
		}
	    });
	fram.add(button, 0, 1, 3, 1);

	button = new JButton(" Show rendezvous !");
	button.addActionListener ( new ActionListener() {
		public void actionPerformed (ActionEvent e) {
		    goShow = true;
		    notifyKernel();
		}
	    });
	fram.add(button, 0, 2, 1, 1);

	button = new JButton("Show objects !");
	button.addActionListener ( new ActionListener() {
		public void actionPerformed (ActionEvent e) {
		    goPrint = true;
		    notifyKernel();
		}
	    });
	fram.add(button, 1, 2, 1, 1);

	button = new JButton("Help !");
	button.addActionListener ( new ActionListener() {
		public void actionPerformed (ActionEvent e) {
		    goHelp = true;
		    notifyKernel();
		}
	    });
	fram.add(button, 2, 2, 1, 1);

	area = new JTextArea("",30,60);
	area.setEditable(false);
	fram.add(area, 0, 0, 3, 1);  // position 0,1 on the frame  
	fram.pack();                 //     width 2, height 1
	fram.setVisible(true);	

	new Thread(this).start();
    }

    synchronized void notifyKernel() {
	notify();
    }

    public synchronized void enqueue (Select sel) {
	selectList.add(sel);
	goShow = true;
	notify();  // restart the kernel thread
    }

    void build () {  // create the list of ready  rendezvous
	int[] itsIndex = new int[1];
	Select currentSelect;
	rendezvousList = new LinkedList();
	Rendezvous rdv;
	// match called methods of 'select' with accepted methods of their respective receivers
	ListIterator selectIter = (ListIterator)selectList.iterator();
	while(selectIter.hasNext()) {
	    currentSelect = (Select)selectIter.next();
	    rdv = null;
	    for (int i = 0; i < currentSelect.called; i++) {
		int methodId = (int)((currentSelect.calledMethods[i] & 0x7fffffffl) >> 16);
		Select s = currentSelect.receivers[i].pending.next;
		if (s != null) {
		    if (s.acceptRequest
			(methodId,
			 (currentSelect.a$oCopy==null)?null:currentSelect.a$oCopy.owner,
			 itsIndex
			 )
			){
			rdv = new Rendezvous(currentSelect, i, s, itsIndex[0]);
			rendezvousList.add(rdv);
		    }
		}
	    }
	    for (int i = 0; i<currentSelect.nbTimeouts; i++) {
		rdv = new Rendezvous(currentSelect, (int)currentSelect.timeoutIds[i], null, -1);
		rendezvousList.add(rdv);
	    }
	    if (currentSelect.defaultId>=0) {
		rdv = new Rendezvous(currentSelect, currentSelect.defaultId, null, -2);
		rendezvousList.add(rdv);
	    }
	    if (rdv==null)
		rendezvousList.add(new Rendezvous(currentSelect, 0, null, 0));
	}    
    }

    boolean showing = false;
    void print () {  // print the list of ready rendezvous
	Rendezvous current;
	Select currentSelect, previous=null;
	int iCall;

	showing = true;
	area.setText("");
        ListIterator rvIter = (ListIterator)rendezvousList.iterator();
	while (rvIter.hasNext()) {
	    current = (Rendezvous)rvIter.next();
	    currentSelect = current.caller;
	    iCall = current.callerI;
	    TreeSet ts = new TreeSet();
	    if (currentSelect!=previous){
		if (currentSelect.a$oCopy==null)
		    area.append("(system");
		else {
		    area.append(currentSelect.a$oCopy.owner+" l: "+currentSelect.location+" r:( ");
		    for (int j=0; j<currentSelect.enabled; j++) {
			ts.add(new Integer((int)currentSelect.enabledMethods[j] & 0xffff));
		    }
		    for (int j=0; j<currentSelect.called; j++) {
			ts.add(new Integer((int)currentSelect.calledMethods[j] & 0xffff));
		    }
		    for (Iterator it=ts.iterator(); it.hasNext();)
			area.append(((Integer)it.next()).toString()+" ");
		}
		area.append(")\n");
		previous = currentSelect;
	    }
	    if (current.acceptor!=null) {
		int methodId = (int)((currentSelect.calledMethods[iCall] & 0x7fffffffl) >> 16);
		int caseId = (int) (currentSelect.calledMethods[iCall] & 0xffff);
		int fVal = (int)((currentSelect.calledMethods[iCall]>>32)&0x7fffffffl);
		area.append("    c:"+caseId+" f:"+fVal);
		currentSelect = current.acceptor;
		int iAcc = current.acceptorI;
		long itsMask = currentSelect.enabledMethods[iAcc];
		int itsForallVal = (int)((itsMask >> 32) & 0x7fffffffl);
		int itsCaseId = (int) (itsMask & 0xffff);
		area.append(" meets "+currentSelect.a$oCopy.owner+" c:"+itsCaseId+" f:"+itsForallVal+"\n");
	    } else if (current.acceptorI ==  -1) {
		area.append("    t:"+current.callerI+"\n");
	    } else if (current.acceptorI == -2) {
		area.append("    d:"+current.callerI+"\n");
	    }
	}
    }

    

    Rendezvous selectRendezvous (int n) {  // return the rendezvous selected by the user
	int i=-1;
	Rendezvous rdv=null;
	Select currentSelect=null, previous=null;

	ListIterator rvIter = (ListIterator)rendezvousList.iterator();
	while ((rvIter.hasNext())&&(i<n)) {
	    rdv = (Rendezvous)rvIter.next();
	    currentSelect = rdv.caller;
	    if (currentSelect!=previous){
		i++;
		previous = currentSelect;
	    }
	    if ( (rdv.acceptor!=null) || (rdv.acceptorI<0) ) {
		i++;
	    }
	}
	if ((i!=n) || ((rdv.acceptor==null)&&(rdv.acceptorI>=0)) )
	    return null;
	return rdv;

    }

    void resume(Rendezvous rdv) {  // resume a pair of active objects or a timeout
	selectList.remove(rdv.caller);
	selectList.remove(rdv.acceptor);
	if (rdv.acceptorI>=0) {
	    if ((rdv.caller.prev.next = rdv.caller.next) != null)
		rdv.caller.next.prev = rdv.caller.prev;
	    if (rdv.caller.a$oCopy!=null)
		rdv.caller.a$oCopy.forall$Value = (int)((rdv.caller.calledMethods[rdv.callerI]>>32) & 0x7fffffffl);
	    Select tmp = rdv.caller.discard((int)(rdv.caller.calledMethods[rdv.callerI] & 0xffffl), rdv.acceptor);
	    if (rdv.acceptor.a$oCopy!=null)
		rdv.acceptor.a$oCopy.forall$Value = (int)((rdv.acceptor.enabledMethods[rdv.acceptorI] >> 32) & 0x7fffffffl);
	    if ((rdv.acceptor.prev.next = rdv.acceptor.next) != null)
		rdv.acceptor.next.prev = rdv.acceptor.prev;
	    rdv.acceptor.discard((int)(rdv.acceptor.enabledMethods[rdv.acceptorI] & 0xffffl));
	    tmp.notif();
	} else {
	    Select tmp = rdv.caller.discard((int)(rdv.callerI));
	    tmp.notif();
	}
    }

    void printObjects () {
	area.setText("");
	ListIterator selectIter = (ListIterator)selectList.iterator();
	while(selectIter.hasNext()) {
	    Select currentSelect = (Select)selectIter.next();
	    if (currentSelect.a$oCopy!=null) {
		Object o = currentSelect.a$oCopy.owner;
		area.append(o+" (line "+currentSelect.location+")\n");
		Class c = o.getClass();
		Field[] publicFields = c.getFields();
		for (int k = 0; k < publicFields.length; k++) {
		    try {
			String fieldName = publicFields[k].getName();
			if ( (fieldName.indexOf("a$o")==-1)
			     && (fieldName.indexOf("a$m")==-1)
			     && (fieldName.indexOf("$SJID")==-1)) {
			    Class typeClass = publicFields[k].getType();
			    String fieldType = typeClass.getName();
			    Object o1 = publicFields[k].get(o);
			    area.append("   "+fieldName+" = "+o1+"\n");
			}
		    } catch (IllegalAccessException e) {
			System.out.println(e);
		    }
		}
	    }
	}
    }

    void help () {
	area.setText("");
	area.append("   ! The name of the objects can be specified by the programmer\n");
	area.append("     by defining a public method 'toString()' in the objects.\n");
	area.append("\n");
	area.append("   ! You must compile with 'sjavac -label ...' to get the line numbers.\n");
	area.append("\n");
	area.append("   In order to trigger a rendezvous, click on the line or\n");
	area.append("   select a few characters of that line and click the button below\n");
	area.append("\n");
	area.append("   Show rendezvous !  displays:\n");
	area.append("\n");
	area.append("   object_name  l: line number where the object is waiting\n");
	area.append("                r: (list of the case numbers of the ready statements)\n");
	area.append("     c: (case number of a ready call)\n");
	area.append("     f: forall variable value (if any)\n");
	area.append("     meets callee_identifier\n");
	area.append("     c: corresponding case of the callee\n");
	area.append("     f: corresponding forall value in the callee (if any)\n");
	area.append("\n");
	area.append("   Show objects ! displays:\n");
	area.append("\n");
	area.append("   object_name (line number where the object is waiting)\n");
	area.append("      the public attributes of the object\n");
	area.append("      ( if the attribute is not a simple type, put 'toString' in the class\n");
	area.append("        that defines the attribute )\n");
	area.append("\n");
    }

    public synchronized void run () {
	for (;;) {
	    try {
		while ((!goStart) && (!goShow) && (!goPrint) && (!goHelp)) {
		    wait ();
		}
		if (goStart){
		    if (showing) {
			goStart = false;
			int caretPos = area.getCaretPosition();
			String st = area.getText();
			int lineNb = 0;
			for (int txtChar=0; txtChar<caretPos; txtChar++) {
			    if (st.charAt(txtChar)=='\n')
				lineNb++;
			}
			Rendezvous rdv = selectRendezvous(lineNb);
			if (rdv==null)
			    area.append("Not a valid position\n");
			else
			    resume(rdv);
		    }
		    goStart = false;
		} else if (goShow){
		    build();
		    print(); // sets showing true
		    goShow = false;
		} else if (goPrint){
		    printObjects();
		    goPrint = false;
		    showing = false;
		} else if (goHelp) {
		    help();
		    goHelp = false;
		    showing = false;
		}
	    } catch (InterruptedException e) {}
	}
    }
}














