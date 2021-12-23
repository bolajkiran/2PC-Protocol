#!/bin/bash


echo "\n******************* TWO PHASE COMMIT PROTOCOL **********************"
echo "*                                                                    *"
echo "* We are executing test case 5 of 2PC protocol                       *"
echo "*                                                                    *"
echo "* Description: If all the participants VOTE-COMMIT, and              *"
echo "* the coordinator failed before sending GLOBAL_COMMIT,               *"
echo "* then participants will wait till the coordinator is recovered      *"
echo "* (since no participant knows the transactions state) and            *"
echo "* when coordinator recovers, it sends GLOBAL_COMMIT to all the       *"
echo "* participants.                                                      *"
echo "*                                                                    *"
echo "**********************************************************************"

echo "\nWe will work with 1 Coordinator and 3 Participants!"

printf "\n"

#gnome-terminal -- java Coordinator 1 

for var in $(seq 0 3)
do
   gnome-terminal -- java TwoPCProtocol $var 
done

wait

