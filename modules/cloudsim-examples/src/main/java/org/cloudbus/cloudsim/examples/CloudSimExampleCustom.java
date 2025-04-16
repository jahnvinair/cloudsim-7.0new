/*
 * Title:        CloudSim Toolkit
 * Description:  CloudSim (Cloud Simulation) Toolkit for Modeling and Simulation
 *               of Clouds
 * Licence:      GPL - http://www.gnu.org/copyleft/gpl.html
 *
 * Copyright (c) 2009, The University of Melbourne, Australia
 */

 package org.cloudbus.cloudsim.examples;

 import java.text.DecimalFormat;
 import java.util.ArrayList;
 import java.util.Calendar;
 import java.util.LinkedList;
 import java.util.List;

 import org.cloudbus.cloudsim.Cloudlet;
 import org.cloudbus.cloudsim.CloudletSchedulerTimeShared;
 import org.cloudbus.cloudsim.Datacenter;
 import org.cloudbus.cloudsim.DatacenterBroker;
 import org.cloudbus.cloudsim.DatacenterCharacteristics;
 import org.cloudbus.cloudsim.Host;
 import org.cloudbus.cloudsim.Log;
 import org.cloudbus.cloudsim.Pe;
 import org.cloudbus.cloudsim.Storage;
 import org.cloudbus.cloudsim.UtilizationModel;
 import org.cloudbus.cloudsim.UtilizationModelFull;
 import org.cloudbus.cloudsim.Vm;
 import org.cloudbus.cloudsim.VmAllocationPolicySimple;
 import org.cloudbus.cloudsim.VmSchedulerTimeShared;
 import org.cloudbus.cloudsim.core.CloudSim;
 import org.cloudbus.cloudsim.provisioners.BwProvisionerSimple;
 import org.cloudbus.cloudsim.provisioners.PeProvisionerSimple;
 import org.cloudbus.cloudsim.provisioners.RamProvisionerSimple;
 
 /**
  * A custom CloudSim example demonstrating a scalable simulation with two datacenters,
  * multiple hosts, VMs, and cloudlets. It showcases resource allocation and task scheduling.
  */
 public class CloudSimExampleCustom {
     private static DatacenterBroker broker;
     private static List<Cloudlet> cloudletList;
     private static List<Vm> vmList;
 
     /**
      * Creates VMs with specified parameters.
      * @param userId The ID of the user/broker.
      * @param vms The number of VMs to create.
      * @return List of created VMs.
      */
     private static List<Vm> createVM(int userId, int vms) {
         List<Vm> list = new ArrayList<>();
         long size = 10000; // image size (MB)
         int ram = 1024; // VM memory (MB)
         int mips = 1000;
         long bw = 1000;
         int pesNumber = 1; // number of CPUs
         String vmm = "Xen"; // VMM name
 
         for (int i = 0; i < vms; i++) {
             list.add(new Vm(i, userId, mips, pesNumber, ram, bw, size, vmm, new CloudletSchedulerTimeShared()));
         }
         return list;
     }
 
     /**
      * Creates cloudlets with specified parameters.
      * @param userId The ID of the user/broker.
      * @param cloudlets The number of cloudlets to create.
      * @return List of created cloudlets.
      */
     private static List<Cloudlet> createCloudlet(int userId, int cloudlets) {
         List<Cloudlet> list = new ArrayList<>();
         long length = 100000;
         long fileSize = 300;
         long outputSize = 300;
         int pesNumber = 1;
         UtilizationModel utilizationModel = new UtilizationModelFull();
 
         for (int i = 0; i < cloudlets; i++) {
             Cloudlet cloudlet = new Cloudlet(i, length, pesNumber, fileSize, outputSize, utilizationModel, utilizationModel, utilizationModel);
             cloudlet.setUserId(userId);
             list.add(cloudlet);
         }
         return list;
     }
 
     /**
      * Creates a datacenter with specified name and host configurations.
      * @param name The name of the datacenter.
      * @return The created datacenter.
      */
     private static Datacenter createDatacenter(String name) {
         List<Host> hostList = new ArrayList<>();
         int mips = 2000;
 
         // Create hosts with varying PE counts
         for (int i = 0; i < 2; i++) {
             List<Pe> peList = new ArrayList<>();
             int peCount = (i == 0) ? 4 : 2; // First host: 4 PEs, Second host: 2 PEs
             for (int j = 0; j < peCount; j++) {
                 peList.add(new Pe(j, new PeProvisionerSimple(mips)));
             }
 
             int ram = 4096; // host memory (MB)
             long storage = 1000000; // host storage
             int bw = 10000;
 
             hostList.add(
                 new Host(
                     i,
                     new RamProvisionerSimple(ram),
                     new BwProvisionerSimple(bw),
                     storage,
                     peList,
                     new VmSchedulerTimeShared(peList)
                 )
             );
         }
 
         String arch = "x86";
         String os = "Linux";
         String vmm = "Xen";
         double time_zone = 10.0;
         double cost = 3.0;
         double costPerMem = 0.05;
         double costPerStorage = 0.1;
         double costPerBw = 0.1;
         LinkedList<Storage> storageList = new LinkedList<>();
 
         DatacenterCharacteristics characteristics = new DatacenterCharacteristics(
             arch, os, vmm, hostList, time_zone, cost, costPerMem, costPerStorage, costPerBw
         );
 
         Datacenter datacenter = null;
         try {
             datacenter = new Datacenter(name, characteristics, new VmAllocationPolicySimple(hostList), storageList, 0);
         } catch (Exception e) {
             e.printStackTrace();
         }
         return datacenter;
     }
 
     /**
      * Prints the cloudlet execution results.
      * @param list The list of cloudlets.
      */
     private static void printCloudletList(List<Cloudlet> list) {
         String indent = "    ";
         Log.println();
         Log.println("========== OUTPUT ==========");
         Log.println("Cloudlet ID" + indent + "STATUS" + indent +
                     "Data center ID" + indent + "VM ID" + indent + "Time" + indent +
                     "Start Time" + indent + "Finish Time");
 
         DecimalFormat dft = new DecimalFormat("###.##");
         for (Cloudlet cloudlet : list) {
             Log.print(indent + cloudlet.getCloudletId() + indent + indent);
             if (cloudlet.getStatus() == Cloudlet.CloudletStatus.SUCCESS) {
                 Log.print("SUCCESS");
                 Log.println(indent + indent + cloudlet.getResourceId() + indent + indent + indent +
                             cloudlet.getGuestId() + indent + indent + dft.format(cloudlet.getActualCPUTime()) +
                             indent + indent + dft.format(cloudlet.getExecStartTime()) + indent + indent +
                             dft.format(cloudlet.getExecFinishTime()));
             } else {
                 Log.println("FAILED");
             }
         }
     }
 
     public static void main(String[] args) {
         Log.println("Starting CloudSimExampleCustom...");
 
         try {
             // Initialize CloudSim
             int num_user = 1;
             Calendar calendar = Calendar.getInstance();
             boolean trace_flag = false;
             CloudSim.init(num_user, calendar, trace_flag);
 
             // Create two datacenters
             Datacenter datacenter0 = createDatacenter("Datacenter_0");
             Datacenter datacenter1 = createDatacenter("Datacenter_1");
 
             // Create broker
             broker = new DatacenterBroker("Broker");
             int brokerId = broker.getId();
 
             // Create VMs and cloudlets
             vmList = createVM(brokerId, 10); // 10 VMs
             cloudletList = createCloudlet(brokerId, 20); // 20 cloudlets
 
             // Submit VMs and cloudlets to broker
             broker.submitGuestList(vmList);
             broker.submitCloudletList(cloudletList);
 
             // Bind cloudlets to VMs
             for (int i = 0; i < cloudletList.size(); i++) {
                 broker.bindCloudletToVm(cloudletList.get(i).getCloudletId(), vmList.get(i % vmList.size()).getId());
             }
 
             // Start simulation
             CloudSim.startSimulation();
 
             // Stop simulation and print results
             List<Cloudlet> newList = broker.getCloudletReceivedList();
             CloudSim.stopSimulation();
             printCloudletList(newList);
 
             Log.println("CloudSimExampleCustom finished!");
         } catch (Exception e) {
             e.printStackTrace();
             Log.println("The simulation has been terminated due to an unexpected error");
         }
     }
 }