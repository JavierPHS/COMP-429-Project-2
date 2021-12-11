# COMP-429-Project-2
This program uses Java to implement the Distance Vector Routing Protocol in a 4-server topology

#INSTALLATION

1) In the 'Project_2_DVRP' folder you will see 4 similarly named .txt files. Open them and modify all 4 of them to include the IP of the servers which will run the program.
   It will still work when using the same IP Address, but the ports must be unique/valid.
   Modify the links/costs as needed.
2) The project folder comes with an executable jar file that you can use to run the program through the cmd terminal.
3) In the CMD terminal, navigate to the 'Project_2_DVRP' folder.
4) While in the same directory as the "dvrp.jar" file, use the command "java -jar dvrp.jar" to begin the program
  
##USAGE

As soon as the program begins, use this command to begin the server for each program before using any of the other commands:\
"server -t top.txt -i [interval value here]"    <---- use on server 1\
"server -t top2.txt -i [interval value here]"    <---- use on server 2\
"server -t top3.txt -i [interval value here]"    <---- use on server 3\
"server -t top4.txt -i [interval value here]"    <---- use on server 4
  
The interval value is the amount of time, in seconds, you want the server to wait before sending out automatic routing table updates.
Once this is done, you can use help for assistance on the rest of the program.
  
