package prm393.group8.flowermanagement.service;

import prm393.group8.flowermanagement.entity.Role;
import prm393.group8.flowermanagement.repository.RoleRepository;
import org.springframework.stereotype.Service;

@Service
public class RoleServiceImpl implements RoleService {

    private final RoleRepository roleRepository;

    public RoleServiceImpl(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    @Override
    public Role getByRoleId(int roleId) {
        return roleRepository.findById(roleId).orElse(null);
    }
}
