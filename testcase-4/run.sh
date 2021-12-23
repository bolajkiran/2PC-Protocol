#!/bin/bash


echo "\n******************* TWO PHASE COMMIT PROTOCOL **********************"
echo "*                                                                    *"
echo "* We are executing test case 4 of 2PC protocol                       *"
echo "* Description: If one of the participants fails/stops (P1)           *"
echo "* after sending VOTE_COMMIT, Coordinator will abort the              *" 
echo "* transaction (GLOBAL_COMMIT)                                        *"
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

