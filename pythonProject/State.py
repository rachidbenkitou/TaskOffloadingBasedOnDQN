class State:
    def __init__(self, task_size, task_computational_requirement, task_max_delay,
                 queue_state, transmission_queue_state, available_bandwidth, edge_server_load):
        self.task_size = task_size  # Size of the task in MB
        self.task_computational_requirement = task_computational_requirement  # GHz needed
        self.task_max_delay = task_max_delay  # Maximum tolerated delay in seconds
        self.queue_state = queue_state  # Normalized state of the queue
        self.transmission_queue_state = transmission_queue_state  # Normalized state of the transmission queue
        self.available_bandwidth = available_bandwidth  # Mbps
        self.edge_server_load = edge_server_load  # Normalized load on the edge server

    def __repr__(self):
        return (f"State(Task Size: {self.task_size} MB, "
                f"Computational Requirement: {self.task_computational_requirement} GHz, "
                f"Max Delay: {self.task_max_delay} s, Queue State: {self.queue_state}, "
                f"Transmission Queue State: {self.transmission_queue_state}, "
                f"Bandwidth: {self.available_bandwidth} Mbps, "
                f"Server Load: {self.edge_server_load})")
