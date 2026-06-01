package com.ecampus.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.ecampus.auth.user.UserDetailsRepository;
import com.ecampus.model.Users;

@Repository
public interface UserRepository extends JpaRepository<Users, Long>, UserDetailsRepository {

    List<Users> findByUrole0(String urole0);

    @Query(value = "SELECT * FROM ec2.users WHERE univid = :uname", nativeQuery = true)
    Optional<Users> findWithName(@Param("uname") String uname);

    @Query(value = "SELECT stdid FROM ec2.users WHERE uname = :username", nativeQuery = true)
    Long findIdByUname(@Param("username") String username);

    @Query(value = "SELECT uid FROM ec2.users WHERE uname = :username", nativeQuery = true)
    Long findUidByUname(@Param("username") String username);

    // Use native query so we hit the actual DB column `univid`
    @Query(value = "SELECT * FROM ec2.users WHERE univid = :univid", nativeQuery = true)
    Optional<Users> findByUnivId(@Param("univid") String univId);

    @Query(value = "SELECT * FROM ec2.users WHERE uname = :uname", nativeQuery = true)
    Optional<Users> findByUname(@Param("uname") String uname);

    @Query(value = """
        SELECT u.*
        FROM ec2.users u
        JOIN ec2.ec2_roles r ON u.urole = r.rid
        WHERE u.univid = :univid
        """,
            nativeQuery = true)
    Optional<Users> findByUnividWithRoles(@Param("univid") String univId);

    // All faculty (urole = '913', row_state > 0) without pagination
    @Query(value = """
        SELECT * FROM ec2.users
        WHERE urole = '913'
        AND ufullname is not null
        AND row_state > 0
        ORDER BY ufullname
        """,
            nativeQuery = true)
    List<Users> findAllFacultyList();

    // Full-text search among faculty (urole = '913', row_state > 0) without pagination
    @Query(value = """
        SELECT * FROM ec2.users
        WHERE urole = '913'
          AND row_state > 0
          AND to_tsvector('english',
                coalesce(uname, '') || ' ' ||
                coalesce(ufullname, '') || ' ' ||
                coalesce(uemail, '')
              ) @@ plainto_tsquery('english', :keyword)
        ORDER BY ufullname
        """,
            nativeQuery = true)
    List<Users> searchFacultyList(@Param("keyword") String keyword);

    @Query(value = """
        SELECT *
        FROM ec2.users
        WHERE urole_0 = 'FACULTY'
          AND LOWER(uemail) LIKE LOWER(CONCAT('%', :query, '%'))
        """,
            nativeQuery = true)
    List<Users> searchFacultyByName(@Param("query") String query);

    List<Users> findByUemailContainingIgnoreCase(String name);

    @Query(value = "select u.uemail,st.stdinstid,CONCAT(st.stdfirstname, ' ', st.stdmiddlename, ' ', st.stdlastname),sd.splname,adp.addcount,adp.addp1,adp.addp2,adp.addp3,adp.addp4,adp.drop1,adp.drop1_p1,adp.drop1_p2,adp.drop1_p3,adp.drop2,adp.drop2_p1,adp.drop2_p2,adp.drop2_p3,adp.drop3,adp.drop3_p1,adp.drop3_p2,adp.drop3_p3 from ec2.adddroppref adp join ec2.students st on adp.sid=st.stdid join ec2.users u on st.stdid=u.stdid join ec2.batches bch on st.stdbchid=bch.bchid join ec2.schemedetails sd on bch.scheme_id=sd.scheme_id and bch.splid=sd.splid", nativeQuery = true)
    List<Object[]> getForAddDrop();
}
