import mpi.MPI;
import mpi.Status;

import java.util.ArrayList;
import java.util.List;

public class Philosopher {
    private final int myRank;
    private final int numberOfProcesses;
    private Fork leftFork;
    private Fork rightFork;
    private List<Message> messages;
    private Message currentMessage;

    public Philosopher(int rank, int numberOfProcesses, Fork leftFork, Fork rightFork) {
        this.myRank = rank;
        this.numberOfProcesses = numberOfProcesses;
        this.leftFork = leftFork;
        this.rightFork = rightFork;
        this.messages = new ArrayList<>();
        think();
    }

    private void think() {
        // think random number of seconds
        System.out.println(tabs() + "Philosopher " + myRank + " is currently thinking.");
        int randomSeconds = 1 + (int) (Math.random() * 10);
        try {
            for (int i = 0; i < randomSeconds; i++) {
                Thread.sleep(1000L);
                // reply to other Philosopher requests
                Status status = MPI.COMM_WORLD.Iprobe(MPI.ANY_SOURCE, 1);
                if (status != null) {
                    //message received
                    Message[] message = new Message[1];
                    status = MPI.COMM_WORLD.Recv(message, 0, 1, MPI.OBJECT, status.source, status.tag);
                    if (message[0].getForkId() == myRank && leftFork.isDirty()) {
                        cleanFork(leftFork);
                        message[0] = new Message(myRank, leftFork.getId(), leftFork);
                        MPI.COMM_WORLD.Isend(message, 0, 1, MPI.OBJECT, status.source, 0);
                        leftFork.setId(-1);
                    } else if (rightFork.isDirty()) {
                        cleanFork(rightFork);
                        message[0] = new Message(myRank, rightFork.getId(), rightFork);
                        MPI.COMM_WORLD.Isend(message, 0, 1, MPI.OBJECT, status.source, 0);
                        rightFork.setId(-1);
                    }
                }
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        while (!haveBothForks()) {
            checkCurrentForks();
            while (true) {
                Message[] message = new Message[1];
                Status status = MPI.COMM_WORLD.Iprobe(MPI.ANY_SOURCE, MPI.ANY_TAG);
                if (status != null) {
                    status = MPI.COMM_WORLD.Recv(message, 0, 1, MPI.OBJECT, MPI.ANY_SOURCE, MPI.ANY_TAG);
                    if (status.tag == 0) {
                        if (message[0].getSource() == currentMessage.getSource()
                                && currentMessage.getForkId() == message[0].getForkId()) {
                            setNewFork(message[0], status.source);
                            currentMessage = null;
                            break;
                        }
                    }  else {
                        giveForkOrSaveMessage(message[0], status.source);
                    }
                }
            }
        }
        // now I can eat (have both Forks)
        eat();
        answerExistingMessages();

        //MPI.Finalize();
    }

    private void checkCurrentForks() {
        if (leftFork.getId() == -1) {
            Message[] message = new Message[1];
            System.out.println(tabs() + "Philosopher " + myRank + " sends request for his left fork.");
            message[0] = new Message(myRank, myRank,null);
            MPI.COMM_WORLD.Isend(message, 0, 1, MPI.OBJECT, (myRank + 1) % numberOfProcesses, 1);
            currentMessage = new Message((myRank + 1) % numberOfProcesses, myRank, null);
        } else if (rightFork.getId() == -1) {
            Message[] message = new Message[1];
            System.out.println(tabs() + "Philosopher " + myRank + " sends request for his right fork.");
            message[0] = new Message(myRank, Math.floorMod((myRank - 1), numberOfProcesses),null);
            MPI.COMM_WORLD.Isend(message, 0, 1, MPI.OBJECT, Math.floorMod((myRank - 1), numberOfProcesses), 1);
            currentMessage = new Message(Math.floorMod((myRank - 1), numberOfProcesses), Math.floorMod((myRank - 1), numberOfProcesses), null);
        }
    }

    private void setNewFork(Message message, int source) {
        if (myRank == message.getForkId()) {
            leftFork.setId(message.getFork().getId());
        } else {
            rightFork.setId(message.getFork().getId());
        }
    }

    private void giveForkOrSaveMessage(Message message, int source) {
        if (!(message.getForkId() == myRank)) {
            if (rightFork.getId() != -1 && rightFork.isDirty()) {
                Message[] forkMessage = new Message[1];
                forkMessage[0] = new Message(myRank, rightFork.getId(), rightFork);
                cleanFork(rightFork);
                sendForkToNeighbourPhilosopher(source, forkMessage);
            } else if (rightFork.getId() != -1 && !rightFork.isDirty()) {
                messages.add(new Message(source, rightFork.getId(), null));
            }
        } else {
            if (leftFork.getId() != -1 && leftFork.isDirty()) {
                Message[] forkMessage = new Message[1];
                forkMessage[0] = new Message(myRank, leftFork.getId(), leftFork);
                cleanFork(leftFork);
                sendForkToNeighbourPhilosopher(source, forkMessage);
            } else if (leftFork.getId() != -1 && !leftFork.isDirty()) {
                messages.add(new Message(source, leftFork.getId(), null));
            }
        }
    }

    private void cleanFork(Fork fork) {
        fork.setDirty(false);
    }

    private void sendForkToNeighbourPhilosopher(int source, Message[] forkMessage) {
        // send fork to neighbour philosopher
        MPI.COMM_WORLD.Isend(forkMessage, 0, 1, MPI.OBJECT, source, 0);
        // set my fork to -1
        if (forkMessage[0].getForkId() == Math.floorMod((myRank - 1), numberOfProcesses)) {
            rightFork.setId(-1);
        } else {
            leftFork.setId(-1);
        }
    }

    private void eat() {
        System.out.println(tabs() + "Philosopher " + myRank + " is now eating.");
        int randomSeconds = 1 + (int) (Math.random() * 10);
        try {
            Thread.sleep(randomSeconds * 1000L);
        } catch (InterruptedException ex) {
            ex.getCause();
        }
        leftFork.setDirty(true);
        rightFork.setDirty(true);
    }

    private void answerExistingMessages() {
        if (!messages.isEmpty()) {
            for (Message message : messages) {
                Message[] forkMessages = new Message[1];
                if (message.getForkId() == myRank) {
                    cleanFork(leftFork);
                    forkMessages[0] = new Message(myRank, leftFork.getId(), leftFork);
                } else {
                    cleanFork(rightFork);
                    forkMessages[0] = new Message(myRank, rightFork.getId(), rightFork);
                }
                sendForkToNeighbourPhilosopher(message.getSource(), forkMessages);
            }
            // clear my messages
            messages.clear();
        }

        think();
    }

    private boolean haveBothForks() {
        return leftFork.getId() != -1 && rightFork.getId() != -1;
    }

    private String tabs() {
        return "\t".repeat(Math.max(0, myRank));
    }
}
