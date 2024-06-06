class Action:
    def __init__(self, offload_decision, edge_server_id, transmission_power):
        self.offload_decision = offload_decision  # 0 for local execution, 1 for offloading
        self.edge_server_id = edge_server_id  # ID of the edge server chosen for offloading
        self.transmission_power = transmission_power  # Transmission power level

    def __repr__(self):
        return (f"Action(Offload Decision: {self.offload_decision}, "
                f"Edge Server ID: {self.edge_server_id}, "
                f"Transmission Power: {self.transmission_power})")


    
